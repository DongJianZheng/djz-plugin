package com.djz.generator.plugins;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.xml.*;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

import java.util.List;


/**
 * 设置生成
 */
public class DjzAbstractXmlElementGenerator extends AbstractXmlElementGenerator {

	@Override
	public void addElements(XmlElement parentElement) {
		addBatchInsertSelectiveXml(parentElement);
        addBatchUpdateXml(parentElement);
		addBatchDeleteXml(parentElement);
		addPageListXml(parentElement);


	}

	private void addPageListXml(XmlElement parentElement) {
		// 增加All_COLUMNS_QUERY
		XmlElement sql = new XmlElement("sql");
		sql.addAttribute(new Attribute("id", "All_COLUMNS_QUERY"));
		//在这里添加where条件
		XmlElement selectTrimElement = new XmlElement("trim"); //设置trim标签
		selectTrimElement.addAttribute(new Attribute("prefix", "WHERE"));
		selectTrimElement.addAttribute(new Attribute("prefixOverrides", "AND | OR")); //添加where和and
		StringBuilder sb = new StringBuilder();
		StringBuilder allBaseColumnsb = new StringBuilder();
		for(IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
			XmlElement selectNotNullElement = new XmlElement("if"); //$NON-NLS-1$
			sb.setLength(0);
			sb.append("null != ");
			sb.append(introspectedColumn.getJavaProperty());
			selectNotNullElement.addAttribute(new Attribute("test", sb.toString()));
			sb.setLength(0);
			// 添加and
			sb.append(" AND ");
			// 添加别名t

			sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
			allBaseColumnsb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
			allBaseColumnsb.append(",");
			// 添加等号
			sb.append(" = ");
			sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn));
			selectNotNullElement.addElement(new TextElement(sb.toString()));
			selectTrimElement.addElement(selectNotNullElement);
		}
		sql.addElement(selectTrimElement);
		parentElement.addElement(sql);



		if(!parentElement.getFormattedContent(0).contains("Base_Column_List")){
			// 公用include
			XmlElement include = new XmlElement("sql");
			include.addAttribute(new Attribute("id", "Base_Column_List"));
			include.addElement(new TextElement(allBaseColumnsb.toString().substring(0,allBaseColumnsb.length()-1)));
			parentElement.addElement(include);

		}

		// 公用select
		sb.setLength(0);
		sb.append("select");
		sb.append(" <include refid=\"Base_Column_List\" />  ");
		sb.append("from ");
		sb.append(introspectedTable.getFullyQualifiedTableNameAtRuntime());
		sb.append(" ");
		TextElement selectText = new TextElement(sb.toString());

		// 公用include
		XmlElement include = new XmlElement("include");
		include.addAttribute(new Attribute("refid", "All_COLUMNS_QUERY"));
		// 增加pageList
		XmlElement pageList = new XmlElement("select");
		pageList.addAttribute(new Attribute("id", "selectList"));
		pageList.addAttribute(new Attribute("resultMap", "BaseResultMap"));
		pageList.addAttribute(new Attribute("parameterType", introspectedTable.getBaseRecordType()));
		pageList.addElement(selectText);
		pageList.addElement(include);
		parentElement.addElement(pageList);
	}

	public void addBatchInsertSelectiveXml(XmlElement parentElement) {
		List<IntrospectedColumn> columns = introspectedTable.getAllColumns();

		String incrementField = introspectedTable.getTableConfiguration().getProperties().getProperty("incrementField");
		if (incrementField != null) {
			incrementField = incrementField.toUpperCase();
		}
		XmlElement javaPropertyAndDbType = new XmlElement("trim");
		javaPropertyAndDbType.addAttribute(new Attribute("prefix", " ("));
		javaPropertyAndDbType.addAttribute(new Attribute("suffix", ")"));
		javaPropertyAndDbType.addAttribute(new Attribute("suffixOverrides", ","));

		XmlElement insertBatchElement = new XmlElement("insert");
		insertBatchElement.addAttribute(new Attribute("id", "insertBatchSelective"));
		insertBatchElement.addAttribute(new Attribute("parameterType", "java.util.List"));

		XmlElement trim1Element = new XmlElement("trim");
		trim1Element.addAttribute(new Attribute("prefix", "("));
		trim1Element.addAttribute(new Attribute("suffix", ")"));
		trim1Element.addAttribute(new Attribute("suffixOverrides", ","));
		for (IntrospectedColumn introspectedColumn : columns) {
			String columnName = introspectedColumn.getActualColumnName();
			if (!columnName.toUpperCase().equals(incrementField)) {
				XmlElement iftest = new XmlElement("if");
				iftest.addAttribute(new Attribute("test", "list[0]." + introspectedColumn.getJavaProperty() + "!=null"));
				iftest.addElement(new TextElement(columnName + ","));
				trim1Element.addElement(iftest);
				XmlElement trimiftest = new XmlElement("if");
				trimiftest.addAttribute(new Attribute("test", "item." + introspectedColumn.getJavaProperty() + "!=null"));
				trimiftest.addElement(new TextElement("#{item." + introspectedColumn.getJavaProperty() + ",jdbcType=" + introspectedColumn.getJdbcTypeName() + "},"));
				javaPropertyAndDbType.addElement(trimiftest);
			}
		}

		XmlElement foreachElement = new XmlElement("foreach");
		foreachElement.addAttribute(new Attribute("collection", "list"));
		foreachElement.addAttribute(new Attribute("index", "index"));
		foreachElement.addAttribute(new Attribute("item", "item"));
		foreachElement.addAttribute(new Attribute("separator", ","));
		insertBatchElement.addElement(new TextElement("insert into " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));
		insertBatchElement.addElement(trim1Element);
		insertBatchElement.addElement(new TextElement(" values "));
		foreachElement.addElement(javaPropertyAndDbType);
		insertBatchElement.addElement(foreachElement);

		parentElement.addElement(insertBatchElement);
	}

    public void addBatchUpdateXml(XmlElement parentElement) {
        List<IntrospectedColumn>  columns = introspectedTable.getAllColumns();
		String keyColumn = "";
		try {
			keyColumn = ((IntrospectedColumn)introspectedTable.getPrimaryKeyColumns().get(0)).getActualColumnName();
		}catch (Exception e){
        	return;
		}


        XmlElement insertBatchElement = new XmlElement("update");
        insertBatchElement.addAttribute(new Attribute("id", "updateBatchByPrimaryKeySelective"));
        insertBatchElement.addAttribute(new Attribute("parameterType", "java.util.List"));

        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "list"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("index", "index"));
        foreach.addAttribute(new Attribute("separator", ";"));

        foreach.addElement(new TextElement("update " + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        XmlElement trim1Element = new XmlElement("set");
        String columnName;
        for (IntrospectedColumn introspectedColumn : columns) {
            columnName = introspectedColumn.getActualColumnName();
            if (!columnName.toUpperCase().equalsIgnoreCase(keyColumn)) {
                XmlElement ifxml = new XmlElement("if");
                ifxml.addAttribute(new Attribute("test", "item." + introspectedColumn.getJavaProperty() + "!=null"));
                ifxml.addElement(new TextElement(columnName + "=#{item." + introspectedColumn.getJavaProperty() + ",jdbcType=" + introspectedColumn.getJdbcTypeName() + "},"));
                trim1Element.addElement(ifxml);
            }
        }
        foreach.addElement(trim1Element);

        foreach.addElement(new TextElement("where "));
        int index = 0;
        for (IntrospectedColumn i : introspectedTable.getPrimaryKeyColumns()) {
            foreach.addElement(new TextElement((index > 0 ? " AND " : "") + i.getActualColumnName() + " = #{item." + i.getJavaProperty() + ",jdbcType=" + i.getJdbcTypeName() + "}"));
        }

        insertBatchElement.addElement(foreach);

        parentElement.addElement(insertBatchElement);
    }

	/**
	 * 批量删除的xml方法生成
	 * @param parentElement
	 */
	private void addBatchDeleteXml(XmlElement parentElement){
		String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();

		String key = "";
		try {
			key = ((IntrospectedColumn)introspectedTable.getPrimaryKeyColumns().get(0)).getActualColumnName();
		}catch (Exception e){
			return;
		}
		String baseSql = "delete from "+ introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()+" where  "+key+" in (";

		FullyQualifiedJavaType paramType = introspectedTable.getPrimaryKeyColumns().get(0).getFullyQualifiedJavaType();
		XmlElement deleteElement = new XmlElement("delete");
		deleteElement.addAttribute(new Attribute("id", "deleteBatchByKeys"));
		deleteElement.addAttribute(new Attribute("parameterType", "java.util.List"));

		XmlElement foreach = new XmlElement("foreach");
		foreach.addAttribute(new Attribute("collection", "list"));
		foreach.addAttribute(new Attribute("item", "item"));
		foreach.addAttribute(new Attribute("index", "index"));
		foreach.addAttribute(new Attribute("separator", ","));
		foreach.addElement(new TextElement("#{item}"));


		deleteElement.addElement(new TextElement(baseSql));



		deleteElement.addElement(foreach);

		deleteElement.addElement(new TextElement(")"));

		parentElement.addElement(deleteElement);
	}

}
