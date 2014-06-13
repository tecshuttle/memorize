package net.tomtalk.memorize;

public class Common {
    private int ItemTypeArray[] = { /* priority= */0, /* is_memo= */0 };

    public int[] get_item_type(String type) {
	if (type.compareTo("bug") == 0) {
	    ItemTypeArray[0] = 80;
	    ItemTypeArray[1] = 1; // bug
	} else if (type.compareTo("todo") == 0) {
	    ItemTypeArray[0] = 50;
	    ItemTypeArray[1] = 1; // todo
	} else if (type.compareTo("memo") == 0) {
	    ItemTypeArray[0] = 10;
	    ItemTypeArray[1] = 1; // memo
	} else {
	    ItemTypeArray[0] = 0;
	    ItemTypeArray[1] = 0; // quiz
	}

	return ItemTypeArray;
    }
}

// end file