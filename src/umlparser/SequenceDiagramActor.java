package umlparser;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SequenceDiagramActor {
	private String className;
	private String objectName;
	private String methodName;
	private Map<String, List<String>> methodCallList = new TreeMap<String, List<String>>();
	
	
	public Map<String, List<String>> getMethodCallList() {
		return methodCallList;
	}
	public void setMethodCallList(Map<String, List<String>> methodCallList) {
		this.methodCallList = methodCallList;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

}
