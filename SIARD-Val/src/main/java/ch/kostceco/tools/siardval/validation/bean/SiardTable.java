package ch.kostceco.tools.siardval.validation.bean;

import java.util.List;

import org.jdom2.Element;

public class SiardTable {
	
	private String tableName;
	private List<Element> metadataXMLElements;
	private List<Element> tableXSDElements;
	private List<Element> tableXMLElements;
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public List<Element> getMetadataXMLElements() {
		return metadataXMLElements;
	}
	
	public void setMetadataXMLElements(List<Element> metadataXMLElements) {
		this.metadataXMLElements = metadataXMLElements;
	}
	
	public List<Element> getTableXSDElements() {
		return tableXSDElements;
	}
	
	public void setTableXSDElements(List<Element> tableXSDElements) {
		this.tableXSDElements = tableXSDElements;
	}

	public List<Element> getTableXMLElements() {
		return tableXMLElements;
	}

	public void setTableXMLElements(List<Element> tableXMLElements) {
		this.tableXMLElements = tableXMLElements;
	}
	
}
