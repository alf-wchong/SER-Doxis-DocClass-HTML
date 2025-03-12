package com.sergroup.ps.usa.unipec;

public class TableContent
{
	private final String documentName;
	private final String htmlContent;

	public TableContent(String documentName, String htmlContent)
	{
		this.documentName = documentName;
		this.htmlContent = htmlContent;
	}

	public String getDocumentName()
	{
		return documentName;
	}

	public String getHtmlContent()
	{
		return htmlContent;
	}

}

