/*  
 * Copyright IBM Corp. 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.g11n.pipeline.tools.cli;

import java.io.FileWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.IOException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.ibm.g11n.pipeline.client.BundleData;
import com.ibm.g11n.pipeline.client.ResourceEntryData;
import com.ibm.g11n.pipeline.client.ResourceEntryDataChangeSet;
import com.ibm.g11n.pipeline.client.ServiceClient;
import com.ibm.g11n.pipeline.client.ServiceException;

/**
 * Merge a bundle.
 * 
 * @author Harpreet K Chawla
 */
@Parameters(commandDescription = "Merge translations from the worker instance to the master instance")
final class MergeTranslationsCmd extends BundleCmd {
    @Parameter(names = { "-m",
            "--master-json-credentials" }, description = "JSON file containing credentials of the master service instance", required = true)
    private String masterJsonCreds;

    @Parameter(names = { "-bd", "--bundles" }, description = "Bundle IDs")
    private String bundleListParam;

    @Parameter(names = { "-d", "--dry-run" }, description = "Dry run")
    boolean isDryRun = false;

    @Parameter(names = { "-c", "--change-log-json" }, description = "Output change log file in JSON format")
    private String changeLogFile;

    @Parameter(names = { "-a",
            "--update-always" }, description = "Update master tranalation value always even the source value has significantly changed.")
    private boolean updateAlways = false;

    @Parameter(names = { "-r",
            "--includes-reviewed" }, description = "Merge changes even if correponding entries are marked as reviewed in master")
    private boolean includesReviewed = false;

    private static final String CAT_UPDATED_AS_REVIEWED = "Updated/As reviewed";
    private static final String CAT_UPDATED_AS_UNREVIEWED = "Updated/As unreviewed";
    private static final String CAT_SKIPPED_SOURCE_CHANGED = "Skipped/Source changed";
    private static final String CAT_SKIPPED_NO_CHANGES = "Skipped/No changes necessary";
    private static final String CAT_SKIPPED_ALREADY_REVIEWED = "Skipped/Already reviewed";
    private static final String CAT_SKIPPED_NOT_AVAILABLE = "Skipped/Not available";

    private static final String CAT_UPDATED_AS_REVIEWED_OVERWRITTEN = "Updated/As reviewed - Overwritten";

    @Override
    protected void _execute() {
        try {
            System.out.println("Master credentials: " + masterJsonCreds);
            System.out.println("Workbench instance credentials: " + jsonCreds);
            System.out.println("Change log file (JSON): " + changeLogFile);

            if (isDryRun) {
                System.out.println("========================================");
                System.out.println(" This is a dry run!");
                System.out.println(" The master instance won't be updated.");
            }

            ServiceClient masterClient = getClient(masterJsonCreds);
            ServiceClient workClient = getClient();

            Set<String> bundleIds = new TreeSet<>();
            if (bundleListParam != null) {
                String[] bundleIdParams = bundleListParam.split(",");
                for (String param : bundleIdParams) {
                    param = param.trim();
                    if (param.length() > 0) {
                        bundleIds.add(param);
                    }
                }
            } else {
                bundleIds.addAll(workClient.getBundleIds());
            }

            Set<String> masterBundleIds = masterClient.getBundleIds();

            ChangeLog changeLog = new ChangeLog();
            changeLog.bundles = new TreeMap<>();

            for (String bundleId : bundleIds) {
                System.out.println("========================================");
                System.out.println("Processing bundle: " + bundleId);

                BundleData workBundleData = workClient.getBundleInfo(bundleId);
                Set<String> workTrgLangs = workBundleData.getTargetLanguages();
                if (workTrgLangs == null || workTrgLangs.isEmpty()) {
                    System.out.println("[Warning] No target languages found in the work bundle");
                    continue;
                }

                Set<String> masterTrgLangs = Collections.emptySet();

                if (masterBundleIds.contains(bundleId)) {
                    BundleData masterBundleData = masterClient.getBundleInfo(bundleId);
                    Set<String> tmpLangs = masterBundleData.getTargetLanguages();
                    if (tmpLangs == null || tmpLangs.isEmpty()) {
                        System.out.println("[Warning] No target languages found in the master bundle.");
                    } else {
                        masterTrgLangs = tmpLangs;
                    }
                } else {
                    System.out.println("[Warning] The bundle does not exist in the master instance.");
                }

                TreeMap<String, LangChanges> bundleChanges = new TreeMap<>();
                changeLog.bundles.put(bundleId, bundleChanges);

                for (String trgLang : workTrgLangs) {

                    Map<String, ResChangeInfo> updated = new TreeMap<>();
                    Map<String, ResChangeInfo> skipped = new TreeMap<>();
                    LangChanges langChanges = new LangChanges();
                    langChanges.updated = updated;
                    langChanges.skipped = skipped;
                    bundleChanges.put(trgLang, langChanges);

                    Map<String, ResourceEntryData> workResEntries = workClient.getResourceEntries(bundleId, trgLang);
                    Map<String, ResourceEntryData> masterResEntries = Collections.emptyMap();

                    if (masterTrgLangs.contains(trgLang)) {
                        masterResEntries = masterClient.getResourceEntries(bundleId, trgLang);
                    }

                    Map<String, ResourceEntryDataChangeSet> langResChanges = new HashMap<>();
                    int cntReviewed = 0;
                    int cntUnreviewed = 0;
                    int cntAlreadyReviewed = 0;
                    int cntSrcChanged = 0;
                    int cntNoChanges = 0;
                    int cntNotAvailable = 0;
                    int cntReviewedOverwritten = 0;

                    for (Entry<String, ResourceEntryData> wkEntry : workResEntries.entrySet()) {
                        String resKey = wkEntry.getKey();
                        ResourceEntryData wkResData = wkEntry.getValue();

                        ResChangeInfo changeInfo = new ResChangeInfo();

                        ResSummary wkRes = new ResSummary();
                        changeInfo.work = wkRes;

                        wkRes.reviewed = wkResData.isReviewed();
                        wkRes.source = wkResData.getSourceValue();
                        wkRes.translation = wkResData.getValue();

                        ResourceEntryData msResData = masterResEntries.get(resKey);
                        if (msResData == null) {
                            // No corresponding master resource entry
                            changeInfo.category = CAT_SKIPPED_NOT_AVAILABLE;
                            skipped.put(resKey, changeInfo);
                            cntNotAvailable++;
                            continue;
                        }

                        ResSummary msRes = new ResSummary();
                        changeInfo.master = msRes;

                        msRes.reviewed = msResData.isReviewed();
                        msRes.source = msResData.getSourceValue();
                        msRes.translation = msResData.getValue();

                        boolean trsMatch = wkRes.translation.equals(msRes.translation);

                        if (msRes.reviewed) {
                            // Master entry is already reviewed
                            if (!trsMatch && includesReviewed) {
                                // Overwrites the master value with the updated
                                // translation,
                                // forced by the option
                                ResSummary resolved = new ResSummary();
                                changeInfo.resolved = resolved;
                                resolved.reviewed = true;
                                resolved.translation = wkRes.translation;

                                changeInfo.category = CAT_UPDATED_AS_REVIEWED_OVERWRITTEN;
                                updated.put(resKey, changeInfo);
                                cntReviewedOverwritten++;

                                // Add change set data to be written in the GP
                                // instance later
                                ResourceEntryDataChangeSet changeSet = new ResourceEntryDataChangeSet();
                                changeSet.setValue(resolved.translation).setReviewed(Boolean.TRUE);

                                langResChanges.put(resKey, changeSet);
                                continue;
                            } else {
                                // Normal case - master entry already marked as
                                // reviewed remains unchanged.
                                changeInfo.category = CAT_SKIPPED_ALREADY_REVIEWED;
                                skipped.put(resKey, changeInfo);
                                cntAlreadyReviewed++;
                                continue;
                            }
                        }

                        boolean exactSrcMatch = wkRes.source.equals(msRes.source);
                        boolean srcMatch = exactSrcMatch ? true
                                : ApproximateMatcher.matches(wkRes.source, msRes.source);

                        if (!srcMatch && !updateAlways) {
                            // Significant change in source value, so the
                            // translation
                            // in the work copy might be no longer reliable.
                            changeInfo.category = CAT_SKIPPED_SOURCE_CHANGED;
                            skipped.put(resKey, changeInfo);
                            cntSrcChanged++;
                            continue;
                        }

                        if (trsMatch && !exactSrcMatch) {
                            // Translation in the master is already identical to
                            // the work
                            // copy. The source value in the master does not
                            // match the source
                            // value in the work copy exactly, therefore, we
                            // don't update
                            // master entry to reviewed:true.
                            changeInfo.category = CAT_SKIPPED_NO_CHANGES;
                            skipped.put(resKey, changeInfo);
                            cntNoChanges++;
                            continue;
                        }

                        // The master entry will be updated as below
                        ResSummary resolved = new ResSummary();
                        changeInfo.resolved = resolved;
                        resolved.reviewed = exactSrcMatch;
                        resolved.translation = wkRes.translation;
                        if (resolved.reviewed) {
                            changeInfo.category = CAT_UPDATED_AS_REVIEWED;
                            cntReviewed++;
                        } else {
                            changeInfo.category = CAT_UPDATED_AS_UNREVIEWED;
                            cntUnreviewed++;
                        }
                        updated.put(resKey, changeInfo);

                        // Add change set data to be written in the GP instance
                        // later
                        ResourceEntryDataChangeSet changeSet = new ResourceEntryDataChangeSet();
                        changeSet.setValue(resolved.translation).setReviewed(Boolean.valueOf(resolved.reviewed));

                        langResChanges.put(resKey, changeSet);
                    }

                    System.out.println("Change summary for language: " + trgLang);
                    System.out.println("  Updated/As reviewed: " + cntReviewed);
                    System.out.println("  Updated/As reviewed - Overwritten: " + cntReviewedOverwritten);
                    System.out.println("  Updated/As unreviewed: " + cntUnreviewed);
                    System.out.println("  Skipped/No changes necessary: " + cntNoChanges);
                    System.out.println("  Skipped/Source value changed: " + cntSrcChanged);
                    System.out.println("  Skipped/Already reviewed: " + cntAlreadyReviewed);
                    System.out.println("  Skipped/Not available: " + cntNotAvailable);

                    int numChangeEntries = langResChanges.size();

                    if (numChangeEntries > 0) {
                        if (isDryRun) {
                            System.out.println(numChangeEntries + " resource entries will be updated for language ("
                                    + trgLang + ") if not dry run");
                        } else {
                            System.out.println("Updating " + numChangeEntries + " resource entries for language ("
                                    + trgLang + ")");
                            masterClient.updateResourceEntries(bundleId, trgLang, langResChanges, false);
                        }
                    } else {
                        System.out.println("Nothing to update for language(" + trgLang + ")");
                    }
                }
            }

            if (changeLogFile != null) {
                Gson gson = new Gson();
                try {
                    JsonWriter jsonLogWriter = new JsonWriter(new FileWriter(changeLogFile));
                    jsonLogWriter.setIndent("  ");
                    gson.toJson(changeLog, ChangeLog.class, jsonLogWriter);
                    jsonLogWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonChangeLog = gson.toJson(changeLog, ChangeLog.class);
                System.out.println(jsonChangeLog);
            }
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
    }

    static class ResSummary {
        String source;
        String translation;
        boolean reviewed;
    }

    static class ResChangeInfo {
        String category;
        ResSummary master;
        ResSummary work;
        ResSummary resolved;
    }

    static class LangChanges {
        Map<String, ResChangeInfo> updated;
        Map<String, ResChangeInfo> skipped;
    }

    static class ChangeLog {
        Map<String, Map<String, LangChanges>> bundles;
    }
}