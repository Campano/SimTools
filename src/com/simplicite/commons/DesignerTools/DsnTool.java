package com.simplicite.commons.DesignerTools;

import java.util.*;
import com.simplicite.util.*;
import com.simplicite.util.tools.*;
import java.io.IOException;
import org.json.JSONObject;

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
	
	public static byte[] html2pdf(String html) throws IOException{
		String url = "https://wkhtml2pdf.dev.simplicite.io/";
		String user = null;
		String password = null;
		JSONObject postData = new JSONObject();
		postData.put("contents", Tool.toBase64(html));
		String[] headers = {"Content-Type:application/json"};
		String encoding = Globals.BINARY;
		return Tool.readUrlAsByteArray(url, user, password, postData.toString(), headers, encoding);
	}
	
	public static void setRespList(String userId, List<String> newGroupsList, String userModule){
		List<String> oldGroupsList = getRespList(userId);
		
		// remove old unused groups
		for(String oldGroup : oldGroupsList)
			if(!newGroupsList.contains(oldGroup))
				Grant.removeResponsibility(userId, oldGroup);
				
		// add new missing groups
		for(String newGroup : newGroupsList)
			if(!oldGroupsList.contains(newGroup))
				Grant.addResponsibility(userId, newGroup, Tool.getCurrentDate(), null, true, userModule);
	}
	
	public static List<String> getRespList(String userId){
		if(Tool.isEmpty(userId))
			return null;
		Grant g = Grant.getSystemAdmin();
		String[] groups = g.queryFirstColumn("select distinct g.grp_name from m_resp r inner join m_group as g on r.rsp_group_id=g.row_id where r.rsp_login_id="+userId);
		return groups!=null && groups.length>0 ? Arrays.asList(groups) : new ArrayList<String>();
	}
}
