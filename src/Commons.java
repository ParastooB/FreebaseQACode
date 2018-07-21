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
    private Map<String, String> goodTagIDs = new HashMap<>(); //(tagID, tag)

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

    private Set<String> commonSetTwo(String tag1, String tag2){ // the tags
        Map<String, Map<String, List<NTriple>>> commonTags = new HashMap<>(); 
        // Map<String, List<NTriple>> commonPredicates = new HashMap<>();
        Set<String> preds = new HashSet<>();
        commonTwo(commonTags);
        Set<String> one = commonTags.get(tag1).keySet();
        Set<String> two = commonTags.get(tag2).keySet();
        for (String x : one){
            if (two.contains(x)){
                String tempPred1 = commonTags.get(tag1).get(x).get(0).getPredicate();
                String tempPred2 = commonTags.get(tag2).get(x).get(0).getPredicate();
                preds.add(tempPred1);
                preds.add(tempPred2);
                List<NTriple> temp = commonTags.get(tag1).get(x);
                for(NTriple t: temp){
                    this.goodTagIDs.put(t.getSubjectID(),tag1);
                }
                temp = commonTags.get(tag2).get(x);
                for(NTriple t: temp){
                    this.goodTagIDs.put(t.getSubjectID(),tag2);
                }
                // if(commonPredicates.containsKey(tempPred1)){
                //     //commonPredicates.get(tempPred1).addAll(commonTags.get(tag2).get(x));
                //     commonPredicates.get(tempPred1).addAll(commonTags.get(tag1).get(x));
                // }else{
                //     commonPredicates.put(tempPred1,new ArrayList<>());
                //     //commonPredicates.get(tempPred1).addAll(commonTags.get(tag2).get(x));
                //     commonPredicates.get(tempPred1).addAll(commonTags.get(tag1).get(x));
                // }
                // if(commonPredicates.containsKey(tempPred2)){
                //     commonPredicates.get(tempPred2).addAll(commonTags.get(tag2).get(x));
                //     //commonPredicates.get(tempPred2).addAll(commonTags.get(tag1).get(x));
                // }else{
                //     commonPredicates.put(tempPred2,new ArrayList<>());
                //     commonPredicates.get(tempPred2).addAll(commonTags.get(tag2).get(x));
                //     //commonPredicates.get(tempPred2).addAll(commonTags.get(tag1).get(x));
                // }
            }
        }
        commonTags.clear();
        return preds;
    }

/*    public void CommonMap(List<NTriple> finalresult){
        List<Map<String, List<NTriple>>> result = new ArrayList<>();
        Map<String, List<NTriple>> garb = new HashMap<>();
        List<String> result2 = new ArrayList<>();
        List<String> temp = new ArrayList<>();
        for (String x: this.tags.keySet()){
            temp.add(x);
        }
        int s = temp.size();
        int count = 0;
        for (int i = 0; i < s-1; i ++){
            for (int j = i + 1; j < s; j ++){
                System.out.println(temp.get(i)+" - vs. - "+temp.get(j));
                // finalresult.addAll(result2);
                garb = commonMapTwo(temp.get(i), temp.get(j));
                result.add(garb);
                System.out.println(garb.keySet());
                for (String p: garb.keySet()){
                    System.out.println("    "+ p + " --> " + garb.get(p).size());
                    count = count + garb.get(p).size();
                }
            }
        }
        System.out.println(count+" tag triples");
        garb.clear();
        temp.clear();
        result.clear();
    }*/

    public void commonPredicatesSet(Set<String> finalresult){
        Set<String> garb = new HashSet<>();
        List<String> temp = new ArrayList<>();
        for (String x: this.tags.keySet()){
            temp.add(x);
        }
        int s = temp.size();
        int count = 0;
        for (int i = 0; i < s-1; i ++){
            for (int j = i + 1; j < s; j ++){
                System.out.println(temp.get(i)+" - vs. - "+temp.get(j));
                // finalresult.addAll(result2);
                garb = commonSetTwo(temp.get(i), temp.get(j));
                System.out.println(garb);
                for (String p: garb){
                    finalresult.add(p);
                    // System.out.println("    "+ p + " --> " + garb.get(p).size());
                    // count = count + garb.get(p).size();
                }
            }
        }
        garb.clear();
        temp.clear();
    }

    public void goodIDs(Map<String, String> finalresult){
        for (String x: this.goodTagIDs.keySet()){
            finalresult.put(x,this.goodTagIDs.get(x));
        }
    }
}
