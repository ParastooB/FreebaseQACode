import java.io.*;
import java.util.*;

public class Search {
    //---STATIC VARIABLES---
    // This is for every single question. 
    private String answer;
    private FreebaseDBHandler db;
    private Commons cm;
    private Predicates preds;
    private boolean exists, matched = false, check, foundText = false, contained = false;
    private boolean VKIDs = false, lookText = false; //Check both value and key in the tags set returned by TAGME, Check the short text fileds for answer
    private int sizeLimit = 100000 , sizeLimit2 = 50000;

    private Set<String> answerIDs = new HashSet<>();
    private List<String> IDsList = new ArrayList<>(); //placeholder list for nameAlias2IDs method
    private Map<String, NTriple> mediatorTriples = new HashMap<>();

    // Store the names in a seperate table, just so we don't have to search again for repeated ID's
    private Map<String, String> objectIDnames = new HashMap<>();
    private Map<String, String> tags = new HashMap<>(); 
    private Set<String> tagIDs = new HashSet<>();
    private List<NTriple> tagTriples = new ArrayList<>();
    private List<NTriple> comTagTriples = new ArrayList<>();
    // The other tag triples which are connected to strings not objects
    private List<NTriple> otherTagTriples = new ArrayList<>();
    private Set<String> doneWithIDs = new HashSet<>(); 
    private Map<NTriple,List<NTriple>> tag3rdTags = new HashMap<>();
    private List<NTriple> tag2ndTags = new ArrayList<>();

    private NTriple mediatorTriple;
    //matches are saved uniquely based on subject, predicate, mediatorPredicate, object
    private Set<List<String>> matches = new HashSet<>();
    private Set<NTriple> secondMatches = new HashSet<>();  
    private List<String> match = new ArrayList<>();

    private Scanner console = new Scanner(System.in);
    private String input = new String();

    private Set<String> sortablePreds = new HashSet<>();

    private Map<String, List<NTriple>> commonPredicates = new HashMap<>(); // Predicate --> Triples
    private List<NTriple> commonTagTriples = new ArrayList<>();
    private Set<String> commonPreds = new HashSet<>();
    private Map<String, Map<String,Map<String,List<NTriple>>>> allSortable = new HashMap<>();

    public Search(String answer, FreebaseDBHandler db, Map<String, String> tags) {
        this.answer = answer;
        this.db = db;
        this.matched = false;
        this.tags = tags;
        this.mediatorTriples = mediatorTriples;
        this.answerIDs = answerIDs;
        this.exists = false;
        this.check = false;
        // this.lookText = true;
    }


    public void bottomUp(){
        //prepares all freebase IDs with a name or alias matching the answer
        List<NTriple> answerTriples = new ArrayList<>();
        this.answerIDs = this.db.nameAlias2IDs(this.answer, this.IDsList, this.answerIDs);
        if (this.answerIDs.size() == 0){
            System.out.println("Skipping because answer not in freebase"); //prints an empty line for spacing
            System.out.println();
            this.exists = false; // skip to the next question
            return;
        }
        this.exists = true;

        for (String answerID : this.answerIDs) {
            answerTriples = this.db.ID2Triples(answerID, answerTriples);
            if (answerTriples == null) // can this ever happen?
                continue;
            for (NTriple answerTriple : answerTriples) {
                if (this.db.isIDMediator(answerTriple.getObjectID()))
                    this.mediatorTriples.put(answerTriple.getObjectID(), answerTriple);
            }
            answerTriples.clear();
        }
    }

	public void commonTags(){
        cm = new Commons(this.db);
        List<NTriple> allTriples = new ArrayList<>();
        cm.commonPredicatesSet(this.commonPreds,this.tags);
        Map<String, String> goodTagIDs = new HashMap<>();
        cm.goodIDs(goodTagIDs);
        System.out.println("+ + + goodTagIDs: " + goodTagIDs.size());
        threeLayers(goodTagIDs,false);
        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("                No answers were found using the Common Tags Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
	}

    public void sortablePredicates(){
        Map<String,String> allTagIDs = new HashMap<>();
        for (String tag : this.tags.keySet()) {
            this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
            if(VKIDs){
                Set<String> tagIDValues = new HashSet<>();
                this.IDsList.clear();
                tagIDValues = this.db.nameAlias2IDs(this.tags.get(tag), this.IDsList, tagIDValues);
                this.tagIDs.addAll(tagIDValues); // Add also the values of tags!
                tagIDValues.clear();
            }
            for (String tagID : this.tagIDs) {
                allSortable.put((tag + ": " + tagID),sortedPredicates(tag, tagID));
                allTagIDs.put(tagID,tag);
            }
            this.tagIDs.clear();
        }
        threeLayers(allTagIDs,true);
        this.allSortable.clear();
        this.sortablePreds.clear();
        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("            No answers were found using the Sorting Predicates Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
    }

// Top Down Search
    public void topDown(){
        Map<String,String> allTagIDs = new HashMap<>();
        for (String tag : this.tags.keySet()) {
            this.tagIDs = this.db.nameAlias2IDs(tag, this.IDsList, this.tagIDs);
            if(VKIDs){
                Set<String> tagIDValues = new HashSet<>();
                this.IDsList.clear();
                tagIDValues = this.db.nameAlias2IDs(this.tags.get(tag), this.IDsList, tagIDValues);
                this.tagIDs.addAll(tagIDValues); // Add also the values of tags!
                tagIDValues.clear();
            }
            for (String i : this.tagIDs){
                allTagIDs.put(i,tag);
            }
        }
        threeLayers(allTagIDs,false);
        if(! matched){
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
            System.out.println("            No answers were found using the Brute-Force Method");
            System.out.println("*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*/\\*");
        }
        allTagIDs.clear();
    }      

    private void threeLayers(Map<String,String> tagIDs, boolean onlySortable){
        List<NTriple> tagMedTriples = new ArrayList<>();
        Map<String, NTriple> mediatorTagTriples = new HashMap<>();

        // Level 1
        int count = 0;
        String tag;
        if(this.matched)
            return;
        for (String tagID : tagIDs.keySet()) {
            tag = tagIDs.get(tagID);
            if(lookText){
                // Search some other predicates other than Name ot Alias or the ones that actually connected to an object
                this.otherTagTriples = this.db.ID2TriplesFull(tagID, this.otherTagTriples);
                if (this.otherTagTriples == null) // can this happen? an ID has no triple associated with it!
                    continue;
                for (NTriple t: this.otherTagTriples) {
                    if (t.getObjectID().toLowerCase().contains("/"+answer+"/"))
                        continue;
                    if (t.getObjectID().toLowerCase().contains(answer)){
                        System.out.println("    "+t.getPredicate());
                        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        System.out.println(t.getObjectID());
                        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        this.foundText = true;
                    }
                }
                this.otherTagTriples.clear(); 
            }

            if(!onlySortable)
                this.doneWithIDs.add(tagID);
            this.tagTriples = this.db.ID2Triples(tagID,tagTriples);
            if (this.tagTriples == null)
                continue;
            for (NTriple tagTriple : this.tagTriples) {
                if (onlySortable){
                    if(!this.sortablePreds.contains(tagTriple.getPredicate()))
                        continue; // don't check triples that don't have sortable predicates. 
                }
                tagTriple.setSubject(tag);
                this.tag2ndTags.add(tagTriple);
                count++;
                check = false;
                check = isConnectedToAnswer(tag, tagTriple);
                if (!check) 
                    check = isConnectedToAnswerMediator(tag, tagTriple);
            }
            this.tagTriples.clear();
        }
        System.out.println("Layer one triples: " + count);
        count = 0;

        // Level 2
        if(this.matched)
            return;
        for (NTriple tagTriple : this.tag2ndTags){
            String objID = tagTriple.getObjectID();
            if (! this.objectIDnames.containsKey(objID))
                this.objectIDnames.put(objID,this.db.ID2name(objID));
            String tempName = this.objectIDnames.get(objID);
            tagTriple.setObject(tempName);
            List<NTriple> tagtagTriples = new ArrayList<>();
            if (objID == null)
                return; 
            if(lookText){
                // Search some other predicates other than Name ot Alias or the ones that actually connected to an object
                this.otherTagTriples = this.db.ID2TriplesFull(objID, this.otherTagTriples);
                if (this.otherTagTriples == null) // can this happen? an ID has no triple associated with it!
                    return;
                for (NTriple t: this.otherTagTriples) {
                    if (t.getObjectID().toLowerCase().contains("/"+answer+"/"))
                        continue;
                    if (t.getObjectID().toLowerCase().contains(answer)){
                        System.out.println("    "+t.getPredicate() + " ID "+ objID);
                        System.out.println("~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2~2");
                        System.out.println(t.getObjectID());
                        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                        this.foundText = true;
                    }
                }  
            }

            if(this.doneWithIDs.contains(objID))
                continue;
            else
                this.doneWithIDs.add(objID);
            tagtagTriples = this.db.ID2Triples(objID, tagtagTriples);
            if (tagtagTriples == null)
                return;    
            this.tag3rdTags.put(tagTriple,new ArrayList<>());
            for (NTriple tagtagTriple : tagtagTriples) {
                count++;
                tagtagTriple.setSubject(tempName);
                this.tag3rdTags.get(tagTriple).add(tagtagTriple);
                this.check = false;
                this.check = isConnectedToAnswer(tempName, tagtagTriple);
                if(this.check){
                    if (! this.secondMatches.contains(tagTriple)){
                        System.out.printf("     The root triple is : %s \n",tagTriple.toString());
                    }
                    this.secondMatches.add(tagTriple);
                }
                if (!this.check){
                    this.check = isConnectedToAnswerMediator(tempName, tagtagTriple);
                    if(this.check){
                        if (! this.secondMatches.contains(tagTriple)){
                            System.out.printf("     The root triple is : %s \n",tagTriple.toString());
                        }
                        this.secondMatches.add(tagTriple);
                    }
                }
            }
            this.otherTagTriples.clear();
            // tagMedTriples.clear();
            // mediatorTagTriples.clear();
            tagtagTriples.clear();
        }

        System.out.println("Layer two triples: " + count);
        count = 0;

        // Level 3
        if(this.matched)
            return;
        for (NTriple root : this.tag3rdTags.keySet()){
            for(NTriple n : this.tag3rdTags.get(root)){
                String objID = n.getObjectID();
                String tempName = this.db.ID2name(objID);
                n.setObject(tempName);

                List<NTriple> tagtagTriples = new ArrayList<>();
                if (objID == null)
                    return; 
                if(this.doneWithIDs.contains(objID))
                    continue;
                else
                    this.doneWithIDs.add(objID);
                tagtagTriples = this.db.ID2Triples(objID, tagtagTriples);
                if (tagtagTriples == null)
                    return;    
                if (! this.objectIDnames.containsKey(objID))
                    this.objectIDnames.put(objID,db.ID2name(objID));
                for (NTriple tagtagTriple : tagtagTriples) {
                    tagtagTriple.setSubject(tempName);
                    count++;
                    this.check = false;
                    this.check = isConnectedToAnswer(tempName, tagtagTriple);
                    if(this.check){
                        if (! this.secondMatches.contains(n)){
                            System.out.printf("     The 2nd layer triple is : %s \n",n.toString());
                            System.out.printf("             The root triple is : %s \n",root.toString());
                        }
                        this.secondMatches.add(n);
                    }
                    if (!this.check){
                        this.check = isConnectedToAnswerMediator(tempName, tagtagTriple);
                        if(this.check){
                            if (! this.secondMatches.contains(n)){
                                System.out.printf("     The 2nd layer triple is : %s \n",n.toString());
                                System.out.printf("             The root triple is : %s \n",root.toString());
                            }
                            this.secondMatches.add(n);
                        }
                    }
                }
                tagtagTriples.clear();
            }
        }
        System.out.println("Layer three triples: " + count);
        count = 0;
        this.tag3rdTags.clear();
        this.tag2ndTags.clear();
        // this.doneWithIDs.clear();
    }

    // if the object of the tagTriple has an ID matching an answer ID
    private boolean isConnectedToAnswer(String tag, NTriple tagTriple){
        // System.out.println(tag + " " +tagTriple);
        boolean result = false;
        String tripleSub = tagTriple.getSubjectID();
        String tripleObj = tagTriple.getObjectID();
        if (this.answerIDs.contains(tagTriple.getObjectID())) {
            if(!tag.startsWith("TAGID:")) 
                tagTriple.setSubject(tag);
            else{
                if (! this.objectIDnames.containsKey(tripleSub))
                    this.objectIDnames.put(tripleSub,this.db.ID2name(tripleSub));
                tagTriple.setSubject(this.objectIDnames.get(tripleSub));
            }
            tagTriple.setObject(this.answer);
            this.match.add(tagTriple.getSubject());
            this.match.add(tagTriple.getPredicate());
            this.match.add(null);
            this.match.add(tagTriple.getObject());
            if (!this.matches.contains(this.match)) {
                this.matched = true;
                this.matches.add(this.match);

                //not correct for second layer
                // System.out.printf("MATCHED1: %s | %s\n", this.tags.get(tag), tagTriple.toString());
                System.out.printf("The match is : %s \n",this.match);
                System.out.printf("Match is from triple : %s \n",tagTriple);
                // System.out.printf("The ID i s : %s from %d answers\n",tagTriple.getObjectID(),this.answerIDs.size());
            }
            result = true;
        }
        else{
            if(lookText){
                if (!this.objectIDnames.containsKey(tripleObj))
                    this.objectIDnames.put(tripleObj,this.db.ID2name(tripleObj));
                String ans = this.objectIDnames.get(tripleObj).toLowerCase();
                if(ans.contains(this.answer)){
                    System.out.println("The answer is contained in "+ ans + " from the tag " + tagTriple);
                    this.contained = true;
                }
            }
        }
        this.match.clear();
        return result;
    }

    //if the object of the tagTriple has an ID matching a mediator
    private boolean isConnectedToAnswerMediator(String tag, NTriple tagTriple){
        // System.out.println(tag + " " +tagTriple);
        boolean result = false;
        String tripleSub = tagTriple.getSubjectID();
        String tripleObj = tagTriple.getObjectID();
        if (this.mediatorTriples.containsKey(tagTriple.getObjectID())) { 
            if(!tag.startsWith("TAGID:")) 
                tagTriple.setSubject(tag);
            else{
            if (! this.objectIDnames.containsKey(tripleSub))
                this.objectIDnames.put(tripleSub,this.db.ID2name(tripleSub));
            tagTriple.setSubject(objectIDnames.get(tripleSub));
                // tagTriple.setSubject("NONAME " + temp);
            }
            this.mediatorTriple = this.mediatorTriples.get(tagTriple.getObjectID()); //no object name for mediatorTriple because mediator
            this.mediatorTriple.setSubject(this.answer);
            this.match.add(tagTriple.getSubject());
            this.match.add(tagTriple.getPredicate());
            this.match.add(mediatorTriple.getPredicate());
            this.match.add(mediatorTriple.getSubject()); // this should be the object (aka the answer), because we stored answerTriple here the subject is the asswer (aka desired object)
            if (!this.matches.contains(this.match)) {
                this.matched = true;
                this.matches.add(this.match);

                //not correct for second layer
                // System.out.printf("MATCHED2: %s | %s | %s\n", this.tags.get(tag), tagTriple.toString(), this.mediatorTriple.toReverseString());
                System.out.printf("The match is : %s \n",this.match);
                System.out.printf("Match is from triple : %s \n",tagTriple);
                // System.out.printf("The MIDs are : %s \n",String.join(",", mediatorTriples.keySet()));
                // System.out.printf("The ID is : %s from %d mediators\n",tagTriple.getObjectID(), this.mediatorTriples.size());
            }
            result = true;
        }
        else{
            if(lookText){
                String tagObj= tagTriple.getObjectID();
                if (! this.objectIDnames.containsKey(tagObj))
                    this.objectIDnames.put(tagObj,this.db.ID2name(tagObj));
                String ans = this.objectIDnames.get(tagObj).toLowerCase();
                if(ans.contains(this.answer)){
                    System.out.println("The answer is contained in "+ ans + " from the tag " + tagTriple);
                    this.contained = true;
                }
            }
        }
        this.match.clear();
        return result;
    }

// helper static methods
    private static Map<String, NTriple> deepCopyMap(Map<String, NTriple> original){
        Map<String, NTriple> copy = new HashMap<String, NTriple>();
        for (Map.Entry<String, NTriple> entry : original.entrySet()){
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
    }

    private static Set<String> deepCopySet(Set<String> original){
        Set<String> copy = new HashSet<String>();
        for (String entry : original){
            copy.add(entry);
        }
        return copy;
    }

    private static Set<List<String>> deepCopyList(Set<List<String>> original){
        List<String> copy = new ArrayList<String>();
        Set<List<String>> copy2 = new HashSet<List<String>>();

        for (List<String> entry2 : original){
            for (String entry : entry2){
                copy.add(entry);
            }
            copy2.add(copy);
        }
        return copy2;
    }

// Other methods
    public Map<String,Map<String,List<NTriple>>> sortedPredicates(String tag, String tagID){
        Map<String,Map<String,List<NTriple>>> temp = new HashMap<>();
        Map<String,Map<String,List<NTriple>>> temp15 = new HashMap<>();
        Map<String,List<NTriple>> temp2 = new HashMap<>();

        this.tagTriples = this.db.ID2Triples(tagID, this.tagTriples);
        if (this.tagTriples == null) // can this happen? an ID has no triple associated with it!
            return null;
        this.preds = new Predicates(tagID, this.db);
        temp = new HashMap<>();
        temp15 = new HashMap<>();
        this.preds.comparablePredicate(temp);
        this.preds.printPredicate();
        for (String s: temp.keySet()){
            this.sortablePreds.add(s);
            temp2 = new HashMap<>();
            temp2 = temp.get(s);
            temp15.put(s,temp2);
        }
        if(temp.size() == 0){
            // System.out.println("No sortable predicates for " + tag + " with ID " + tagID);
        }else if(temp.size() > 0){
            System.out.println(this.sortablePreds.size() + " sortable predicates, listed below:");
            System.out.println(this.sortablePreds);
        }

        this.preds.cleanUp();
        return temp15;
    }


    public void cleanUp(){
        this.IDsList.clear();
        this.mediatorTriples.clear();
        this.answerIDs.clear();
        this.tags.clear();
        this.objectIDnames.clear();
        this.matches.clear();
        this.secondMatches.clear();
        this.sortablePreds.clear();
        this.commonTagTriples.clear();
        this.doneWithIDs.clear();
        System.gc();
    }

    public boolean isMatched(){
        return this.matched;
    }

    public boolean isAnswer(String objectID){
        return this.answerIDs.contains(objectID);
    }

    public boolean isMedAnswer(String objectID){
        return this.mediatorTriples.containsKey(objectID);
    }

    public boolean isInFB(){
        return this.exists;
    }

    public boolean isAnswerInText(){
    return this.foundText;
    }

    public boolean isAnswerContained(){
    return this.contained;
    }

    public Map<String, NTriple> getMediatorsMap(){
        Map<String, NTriple> copy = deepCopyMap(this.mediatorTriples);
        return copy;
    }

    public Set<String> getAnswerIDsSet(){
        Set<String> copy = deepCopySet(this.answerIDs);
        return copy;
    }

    public Set<List<String>> getMatches(){
        Set<List<String>> copy = deepCopyList(this.matches);
        return copy;
    }

    public int getMatchesSize(){
        return this.matches.size();
    }

    public int getAnswerIDsSize(){
        return this.answerIDs.size();
    }

    public String getQuestionPackage(String question){
        String result = new String(question + " | " + this.answer);
        for (String tag : tags.keySet()) {
            result = result + (" | " + tag);
            result = result + (" | " + tags.get(tag));
        }
        return result;
    }
}