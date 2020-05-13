package com.simplicite.commons.DesignerTools;

import java.util.*;
import com.simplicite.util.*;
import com.simplicite.util.tools.*;

/**
 * Shared code DsnTool
 */
public class DsnTool implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * To be called by getTargetObject
	 */ 
	public static String[] getTargetObjectSimpleNNPattern(String rowId, String[] row, ObjectDB obj, String objA, String fkObjA, String objB, String fkObjB){
		if("1".equals(obj.getParameter("_UI_EDIT_TEMPLATE_"))) // Template editor
			return null;
		if(rowId.equals(ObjectField.DEFAULT_ROW_ID))
			return null;
		if(obj.getParentObject() == null)
			return null;
		if(row==null && (rowId.equals(obj.getRowId()) || obj.select(rowId)))
			row = obj.getValues();
		if(row==null)
			return null;
		if(obj.getParentObject().getName().equals(objA)) //if parent is object A => open object B
			return new String[]{objB, "the_ajax_"+objB, row[obj.getFieldIndex(fkObjB)] };
		if(obj.getParentObject().getName().equals(objB)) // if parent if object B => open object A
			return new String[]{objA, "the_ajax_"+objA, row[obj.getFieldIndex(fkObjA)] };
		return null;		
	}
}
