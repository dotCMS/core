package com.dotcms.api.traversal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Information about a tree node used for traversal.
 */
public class TreeNodeInfo {

    private String site;
    private Set<String> languages;
    private Set<String> liveLanguages;
    private Set<String> workingLanguages;
    private int assetsCount;
    private int foldersCount;
    private Map<String, Integer> liveAssetsCountByLanguage;
    private Map<String, Integer> workingAssetsCountByLanguage;

    /**
     * Constructs a new TreeNodeInfo object.
     */
    public TreeNodeInfo(final String site) {
        this.languages = new HashSet<>();
        this.liveLanguages = new HashSet<>();
        this.workingLanguages = new HashSet<>();
        this.assetsCount = 0;
        this.foldersCount = 0;
        this.liveAssetsCountByLanguage = new HashMap<>();
        this.workingAssetsCountByLanguage = new HashMap<>();

        this.site = site;
    }

    /**
     * Returns the site associated with this TreeNodeInfo object.
     *
     * @return the site
     */
    public String site() {
        return this.site;
    }

    /**
     * Returns the set of languages in the tree node.
     *
     * @return the set of languages
     */
    public Set<String> languages() {
        return this.languages;
    }

    /**
     * Returns the set of live languages in the tree node.
     *
     * @return the set of live languages
     */
    public Set<String> liveLanguages() {
        return this.liveLanguages;
    }

    /**
     * Returns the set of working languages in the tree node.
     *
     * @return the set of working languages
     */
    public Set<String> workingLanguages() {
        return this.workingLanguages;
    }

    /**
     * Returns the count of assets in the tree node.
     *
     * @return the count of assets
     */
    public int assetsCount() {
        return this.assetsCount;
    }

    /**
     * Retrieves the count of live assets by language.
     *
     * @param language the language to retrieve the count for
     * @return the count of live assets for the specified language
     */
    public int liveAssetsCountByLanguage(final String language) {
        return this.liveAssetsCountByLanguage.getOrDefault(language, 0);
    }

    /**
     * Retrieves the count of working assets by language.
     *
     * @param language the language to retrieve the count for
     * @return the count of working assets for the specified language
     */
    public int workingAssetsCountByLanguage(final String language) {
        return this.workingAssetsCountByLanguage.getOrDefault(language, 0);
    }

    /**
     * Returns the count of folders in the tree node.
     *
     * @return the count of folders, or 1 if there are no folders, there is always at least
     * one folder, the root folder
     */
    public int foldersCount() {
        return this.foldersCount == 0 ? 1 : this.foldersCount;
    }

    /**
     * Adds a language to the set of languages.
     *
     * @param language the language to add
     */
    public void addLanguage(String language) {
        this.languages.add(language);
    }

    /**
     * Adds a live language to the set of live languages.
     *
     * @param language the live language to add
     */
    public void addLiveLanguage(String language) {
        this.liveLanguages.add(language);
    }

    /**
     * Adds a working language to the set of working languages.
     *
     * @param language the working language to add
     */
    public void addWorkingLanguage(String language) {
        this.workingLanguages.add(language);
    }

    /**
     * Increments the count of assets by 1.
     */
    public void incrementAssetsCount() {
        this.assetsCount++;
    }

    /**
     * Increments the count of folders by 1.
     */
    public void incrementFoldersCount() {
        this.foldersCount++;
    }

    /**
     * Increments the count of live assets by language.
     *
     * @param language the language to increment the count for
     */
    public void incrementLiveAssetsCountByLanguage(final String language) {
        this.liveAssetsCountByLanguage.put(language,
                this.liveAssetsCountByLanguage(language) + 1);
    }

    /**
     * Increments the count of working assets by language.
     *
     * @param language the language to increment the count for
     */
    public void incrementWorkingAssetsCountByLanguage(final String language) {
        this.workingAssetsCountByLanguage.put(language,
                this.workingAssetsCountByLanguage(language) + 1);
    }

}
