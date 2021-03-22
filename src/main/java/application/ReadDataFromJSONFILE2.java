package application;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public class ReadDataFromJSONFILE2 {

	
	public static List<VariablesJson> getVariables(String json){
		ArrayList<VariablesJson> result = new ArrayList<VariablesJson>();
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		
		List<Map> jsoon = new Gson().fromJson(reader, new TypeToken<List<Map>>() {}.getType());
		for(Map obj : jsoon) {
			String id = (String) ((Map) obj.get("_id")).get("$oid");
			String title = (String) (obj.get("title"));
			String createdAt = (String) (obj.get("createdAt"));
			String text = (String) (obj.get("text"));
			
			result.add(new VariablesJson(id, title, createdAt, text));
		}
		
		return result;
	}
	
	public static VariablesJson getVariable(String json) {
		JsonReader reader = new JsonReader(new StringReader(json));
		reader.setLenient(true);
		
		Map jsonMap = new Gson().fromJson(reader, Map.class);
		String id = (String) ((Map) jsonMap.get("_id")).get("$oid");
		String title = (String) (jsonMap.get("title"));
		String createdAt = (String) (jsonMap.get("createdAt"));
		String text = (String) (jsonMap.get("text"));
		
		return new VariablesJson(id, title, createdAt, text);
	}
}
