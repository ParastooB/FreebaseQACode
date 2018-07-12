import java.io.*;
import java.util.*;

public class Commons {
    //---STATIC VARIABLES---
    // This is for every single question. 
    private FreebaseDBHandler db;
    private Predicates preds;
    private List<String> IDsList = new ArrayList<>(); //placeholder list for nameAlias2IDs method
    private Map<String, String> tags = new HashMap<>(); 
    private Set<String> tagIDs = new HashSet<>();
    private List<NTriple> tagTriples = new ArrayList<>();

    private Map<String, List<NTriple>> commonPredicates = new HashMap<>(); // Predicate --> Triples

    public Commons(FreebaseDBHandler db, Map<String, String> tags) {
        this.db = db;
        this.tags = tags;
    }

    private void commonTwo(Map<String, Map<String, List<NTriple>>> commonTags){
        Set<String> tagIDValues = new HashSet<>(); //not only keys
        for (String tag : this.tags.keySet()) {
            commonTags.put(tag, new HashMap<>());
            this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
            for (String tagID : this.tagIDs) {
                this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
                if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
                    continue;
                for (NTriple tagTriple : this.tagTriples) {
                    if(commonTags.get(tag).containsKey(tagTriple.getObjectID())){
                        if (commonTags.get(tag).get(tagTriple.getObjectID()) == null){
                           commonTags.get(tag).put(tagTriple.getObjectID(),new ArrayList<>());
                        }
                        commonTags.get(tag).get(tagTriple.getObjectID()).add(tagTriple);
                    }
                    else{
                        commonTags.get(tag).put(tagTriple.getObjectID(),new ArrayList<>());
                        commonTags.get(tag).get(tagTriple.getObjectID()).add(tagTriple);
                    }
                }
                this.tagTriples.clear();
            }
            this.tagIDs.clear();
        }
    }

    public void commonMap(Map<String, List<NTriple>> commonPredicates, String tag1, String tag2){ // the tags
        Map<String, Map<String, List<NTriple>>> commonTags = new HashMap<>(); 
        commonTwo(commonTags);
        Set<String> one = commonTags.get(tag1).keySet();
        Set<String> two = commonTags.get(tag2).keySet();
        for (String x : one){
            if (two.contains(x)){
                String tempPred = commonTags.get(tag2).get(x).get(0).getPredicate();
                if(commonPredicates.containsKey(tempPred)){
                    commonPredicates.get(tempPred).addAll(commonTags.get(tag2).get(x));
                    commonPredicates.get(tempPred).addAll(commonTags.get(tag1).get(x));
                }else{
                    commonPredicates.put(tempPred,new ArrayList<>());
                    commonPredicates.get(tempPred).addAll(commonTags.get(tag2).get(x));
                    commonPredicates.get(tempPred).addAll(commonTags.get(tag1).get(x));
                }
            }
        }
        commonTags.clear();
    }
}