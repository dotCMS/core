package com.dotcms.ai.util;

import com.google.common.collect.ImmutableSet;
import io.vavr.Lazy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class StopwordsUtil {

    // twitter stopwords - stopwords_twitter.txt
    private final static Lazy<Set<String>> STOPWORDS  = Lazy.of(()->new TreeSet(new ImmutableSet.Builder<String>().add(
            "2day", "a", "about", "above", "according", "across", "actually", "adj",
            "after", "afterwards", "again", "ahah", "all", "almost", "along", "already", "also", "although", "always",
            "am", "ami", "among", "amongst", "an", "and", "another", "any", "anyhow", "anyone", "anything", "anywhere",
            "are", "aren", "arent", "around", "as", "aswell", "at", "avec", "back", "be", "became", "because", "become",
            "becomes", "been", "beforehand", "begin", "being", "below", "beside", "besides", "between", "billion",
            "billions", "bit", "both", "br", "bro", "bruv", "but", "by", "can", "cannot", "cant", "caption", "cm",
            "cmon", "co", "com", "come", "con", "cos", "could", "couldn", "couldnt", "da", "day", "de", "defo", "dem",
            "despite", "dey", "did", "didn", "didnt", "do", "does", "doesn", "doesnt", "don", "done", "dont", "down",
            "dr", "du", "during", "each", "early", "eg", "eight", "eighteen", "eighty", "either", "eleven", "else",
            "elsewhere", "en", "end", "ending", "enough", "ese", "etc", "etre", "even", "ever", "every", "every1",
            "everybody", "everyone", "everywhere", "except", "fb", "fella", "fellas", "few", "ff", "fifteen", "fifth",
            "fifty", "first", "five", "for", "forty", "found", "four", "fourteen", "fourth", "fr", "from", "further",
            "gd", "get", "go", "gona", "gonna", "got", "gotten", "gud", "guy", "guys", "ha", "had", "hadnt", "hah",
            "haha", "hahah", "hahaha", "hahahaha", "hahahahaha", "has", "hasn", "hasnt", "have", "haven", "havent",
            "he", "heh", "hehe", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "hey",
            "heya", "heyah", "him", "his", "hm", "hmm", "hmmm", "hmmmm", "how", "however", "hoy", "hr", "http", "https",
            "hundred", "hundredth", "i.e.", "ie", "if", "ill", "in", "inc", "inc.", "indeed", "instead", "into", "is",
            "isn", "isnt", "it", "its", "itself", "ive", "je", "just", "know", "la", "lad", "lads", "last", "late",
            "later", "le", "les", "less", "let", "lets", "like", "likely", "ll", "lmao", "lol", "look", "looking",
            "ltd", "ly", "ma", "made", "make", "makes", "many", "may", "maybe", "me", "meantime", "meanwhile", "meh",
            "meme", "mi", "might", "million", "millions", "miss", "moi", "mon", "more", "most", "mostly", "mr", "mrs",
            "ms", "mt", "much", "must", "muy", "my", "myself", "mme", "na", "namely", "near", "need", "neither",
            "never", "nevertheless", "new", "next", "nine", "nineteen", "ninety", "nobody", "non", "none",
            "nonetheless", "noone", "nor", "not", "now", "och", "of", "off", "often", "oh", "ok", "okay", "om", "on",
            "once", "one", "ones", "only", "onto", "ool", "or", "oro", "ot", "other", "others", "otherwise", "our",
            "ours", "ourselves", "out", "over", "own", "pal", "pals", "per", "percent", "perhaps", "plz", "pm", "por",
            "prof", "qu", "que", "quel", "quelle", "rather", "re", "really", "rev", "rly", "rofl", "rt", "said", "same",
            "say", "saying", "says", "se", "second", "see", "seem", "seemed", "seeming", "seems", "seven", "seventeen",
            "seventy", "several", "she", "should", "shouldn", "shouldnt", "simply", "since", "six", "sixteen", "sixty",
            "so", "som", "some", "someone", "soo", "sooo", "sr", "st", "still", "stop", "such", "surely", "taking",
            "tco", "te", "tem", "ten", "tens", "tenth", "th", "than", "that", "thats", "the", "their", "them",
            "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon",
            "these", "they", "third", "thirteen", "thirty", "this", "tho", "those", "though", "thousand", "three",
            "through", "throughout", "thru", "thus", "ti", "til", "till", "to", "todo", "todos", "together", "toi",
            "ton", "too", "toward", "towards", "tweep", "tweeps", "tweet", "twelve", "twenty", "twibbon", "two", "un",
            "una", "under", "une", "unless", "unlike", "unlikely", "until", "up", "upon", "ur", "us", "use", "used",
            "using", "ve", "very", "via", "vs", "wana", "wanna", "was", "wasn", "way", "we", "well", "were", "weren",
            "werent", "weve", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas",
            "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever",
            "whole", "whom", "whomever", "whose", "why", "will", "with", "within", "without", "won", "would", "wouldn",
            "wouldnt", "wow", "wrent", "www", "xx", "xxx", "ya", "yah", "ye", "yea", "yeah", "year", "years", "yeh",
            "yep", "yet", "you", "your", "yours", "yourself", "yourselves", "yr", "za", "tre"
    ).build()));


    public String removeStopwords(String incoming) {
        if (incoming == null) {
            return null;
        }
        incoming=incoming.toLowerCase();

        List<String> response =  Arrays.stream(incoming.split("\\s+")).filter(w->!STOPWORDS.get().contains(w)).collect(Collectors.toList());

        return String.join(" ",response);


    }


}
