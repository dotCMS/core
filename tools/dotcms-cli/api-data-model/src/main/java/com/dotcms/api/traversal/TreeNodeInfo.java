package com.dotcms.api.traversal;

import java.util.HashSet;
import java.util.Set;

/**
 * Information about a tree node used for traversal.
 */
public class TreeNodeInfo {

    private Set<String> languages;
    private Set<String> liveLanguages;
    private Set<String> workingLanguages;
    private int assetsCount;
    private int foldersCount;

    /**
     * Constructs a new TreeNodeInfo object.
     */
    public TreeNodeInfo() {
        this.languages = new HashSet<>();
        this.liveLanguages = new HashSet<>();
        this.workingLanguages = new HashSet<>();
        this.assetsCount = 0;
        this.foldersCount = 0;
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
     * Returns the count of folders in the tree node.
     *
     * @return the count of folders
     */
    public int foldersCount() {
        return this.foldersCount;
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
     * Increments the count of assets by a specified value.
     *
     * @param assetsCount the value to increment the assets count by
     */
    public void incrementAssetsCount(int assetsCount) {
        this.assetsCount += assetsCount;
    }

    /**
     * Increments the count of folders by a specified value.
     *
     * @param foldersCount the value to increment the folders count by
     */
    public void incrementFoldersCount(int foldersCount) {
        this.foldersCount += foldersCount;
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
}
