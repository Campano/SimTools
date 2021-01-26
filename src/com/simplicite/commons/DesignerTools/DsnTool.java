package com.simplicite.commons.DesignerTools;

import java.util.*;
import com.simplicite.util.*;
import com.simplicite.util.tools.*;
import com.simplicite.util.tools.BusinessObjectTool;
import com.simplicite.util.exceptions.*;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Shared code DsnTool
 */
public class DsnTool implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_USER_MODULE="ApplicationUsers";
	
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
	
	@Deprecated
	public static void createObject(ObjectDB obj){
		silentForceCreateObject(obj);
	}

	public static void silentForceCreateObject(ObjectDB obj){
		try{
			forceCreateObject(obj);
		}
		catch(Exception e){
			AppLog.error(DsnTool.class, "createObject", e.getMessage(), e, obj.getGrant());
		}
	}
	
	public static void forceCreateObject(ObjectDB obj) throws CreateException, ValidateException{
		boolean[] crud = obj.getGrant().changeAccess(obj.getName(), true, true, false, false);
		(new BusinessObjectTool(obj)).validateAndCreate();
		obj.getGrant().changeAccess(obj.getName(), crud);
	}

	@Deprecated
	public static void updateObject(ObjectDB obj){
		silentForceUpdateObject(obj);
	}

	public static void silentForceUpdateObject(ObjectDB obj){
		try{
			forceUpdateObject(obj);
		}
		catch(Exception e){
			AppLog.error(DsnTool.class, "updateObject", e.getMessage(), e, obj.getGrant());
		}
	}

	public static void forceUpdateObject(ObjectDB obj) throws UpdateException, ValidateException{
		boolean[] crud = obj.getGrant().changeAccess(obj.getName(), false, true, true, false);
		(new BusinessObjectTool(obj)).validateAndUpdate();
		obj.getGrant().changeAccess(obj.getName(), crud);
	}
	
	public static void upsertObject(ObjectDB obj) throws SearchException, UpdateException, SaveException, ValidateException{
		BusinessObjectTool bot = new BusinessObjectTool(obj);
		
		HashMap<String,String> filters = new HashMap<>();
		for(ObjectField f : obj.getFields())
			if(f.isFunctId())
				filters.put(f.getName(), f.getValue());
		List<String[]> rslt = bot.search(filters);
		
		if(rslt.size()>1)
			throw new SearchException("Functional key not unique");
		else if(rslt.size()==1){
			bot.getObject().setFieldValue("row_id", rslt.get(0)[obj.getFieldIndex("row_id")]);
			bot.validateAndUpdate(false);
		}
		else
			bot.validateAndCreate();
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
	
	public static void setRespList(String userId, List<String> newGroupsList){
		List<String> oldGroupsList = getRespList(userId);
		// remove old unused groups
		for(String oldGroup : oldGroupsList)
			if(!newGroupsList.contains(oldGroup))
				Grant.removeResponsibility(userId, oldGroup);		
		// add new missing groups
		for(String newGroup : newGroupsList)
			if(!oldGroupsList.contains(newGroup))
				Grant.addResponsibility(userId, newGroup, Tool.getCurrentDate(), null, true, DEFAULT_USER_MODULE);
	}
	
	public static List<String> getRespList(String userId){
		if(Tool.isEmpty(userId))
			return null;
		Grant g = Grant.getSystemAdmin();
		String[] groups = g.queryFirstColumn("select distinct g.grp_name from m_resp r inner join m_group as g on r.rsp_group_id=g.row_id where r.rsp_login_id="+userId);
		return groups!=null && groups.length>0 ? Arrays.asList(groups) : new ArrayList<String>();
	}
	
	public static JSONObject getObjectAsJsonTreeview(String objectName, String rowId, String treeviewName, int depth, Grant g) throws GetException{
		ObjectDB object = g.getTmpObject(objectName);
		TreeView tv = g.getTreeView(treeviewName);
		Parameters.TreeviewParam p = new Parameters.TreeviewParam(tv);
		p.setDepth(depth);
		String json = JSONTool.get(object, rowId, ObjectDB.CONTEXT_LIST, false, null, null, true, false, null, false, false, null, p);
		return new JSONObject(json);
	}
	
	//recursive
	public static JSONObject cleanTv(JSONObject jsonTv){
		JSONObject rslt = jsonTv.getJSONObject("item");
		JSONArray links = jsonTv.optJSONArray("links");
		if(links!=null){
			JSONObject childs = new JSONObject();
			for(int i=0; i<links.length(); i++){
				JSONArray list = links.getJSONObject(i).optJSONArray("list");
				if(list!=null && list.length()>0){
					JSONArray arr = new JSONArray();
					for(int j=0; j<list.length(); j++){
						arr.put(cleanTv(list.getJSONObject(j)));
					}
					childs.put(links.getJSONObject(i).getString("object"), arr);
				}
			}
			rslt.put("childs", childs);
		}
		return rslt;
	}
}
