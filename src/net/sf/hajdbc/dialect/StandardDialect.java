/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2006 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.hajdbc.ColumnProperties;
import net.sf.hajdbc.Dialect;
import net.sf.hajdbc.ForeignKeyConstraint;
import net.sf.hajdbc.TableProperties;
import net.sf.hajdbc.UniqueConstraint;
import net.sf.hajdbc.util.Strings;

/**
 * @author  Paul Ferraro
 * @since   1.1
 */
public class StandardDialect implements Dialect
{
	private Pattern selectForUpdatePattern = Pattern.compile(this.selectForUpdatePattern(), Pattern.CASE_INSENSITIVE);
	private Pattern insertIntoTablePattern = Pattern.compile(this.insertIntoTablePattern(), Pattern.CASE_INSENSITIVE);
	private Pattern sequencePattern = Pattern.compile(this.sequencePattern(), Pattern.CASE_INSENSITIVE);

	/**
	 * @see net.sf.hajdbc.Dialect#getSimpleSQL()
	 */
	public String getSimpleSQL()
	{
		return this.executeFunctionSQL(this.currentTimestampFunction());
	}

	protected String executeFunctionFormat()
	{
		StringBuilder builder = new StringBuilder("SELECT {0}");
		
		String dummyTable = this.dummyTable();
		
		if (dummyTable != null)
		{
			builder.append(" FROM ").append(dummyTable);
		}
		
		return builder.toString();
	}
	
	protected String executeFunctionSQL(String function)
	{
		return MessageFormat.format(this.executeFunctionFormat(), function);
	}
	
	protected String currentTimestampFunction()
	{
		return "CURRENT_TIMESTAMP";
	}
	
	protected String dummyTable()
	{
		return null;
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getLockTableSQL(net.sf.hajdbc.TableProperties)
	 */
	public String getLockTableSQL(TableProperties properties) throws SQLException
	{
		StringBuilder builder = new StringBuilder("UPDATE ").append(properties.getName()).append(" SET ");
		
		UniqueConstraint primaryKey = properties.getPrimaryKey();
		
		Collection<String> columnList = (primaryKey != null) ? primaryKey.getColumnList() : properties.getColumns();

		Iterator<String> columns = columnList.iterator();
		
		while (columns.hasNext())
		{
			String column = columns.next();
			
			builder.append(column).append(" = ").append(column);
			
			if (columns.hasNext())
			{
				builder.append(", ");
			}
		}
		
		return builder.toString();
	}

	/**
	 * @see net.sf.hajdbc.Dialect#getTruncateTableSQL(net.sf.hajdbc.TableProperties)
	 */
	public String getTruncateTableSQL(TableProperties properties) throws SQLException
	{
		return MessageFormat.format(this.truncateTableFormat(), properties.getName());
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getCreateForeignKeyConstraintSQL(net.sf.hajdbc.ForeignKeyConstraint)
	 */
	public String getCreateForeignKeyConstraintSQL(ForeignKeyConstraint key)
	{
		return MessageFormat.format(this.createForeignKeyConstraintFormat(), key.getName(), key.getTable(), Strings.join(key.getColumnList(), ", "), key.getForeignTable(), Strings.join(key.getForeignColumnList(), ", "), key.getDeleteRule(), key.getUpdateRule(), key.getDeferrability());
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getDropForeignKeyConstraintSQL(net.sf.hajdbc.ForeignKeyConstraint)
	 */
	public String getDropForeignKeyConstraintSQL(ForeignKeyConstraint key)
	{
		return MessageFormat.format(this.dropForeignKeyConstraintFormat(), key.getName(), key.getTable());
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getCreateUniqueConstraintSQL(net.sf.hajdbc.UniqueConstraint)
	 */
	public String getCreateUniqueConstraintSQL(UniqueConstraint constraint)
	{
		return MessageFormat.format(this.createUniqueConstraintFormat(), constraint.getName(), constraint.getTable(), Strings.join(constraint.getColumnList(), ", "));
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getDropUniqueConstraintSQL(net.sf.hajdbc.UniqueConstraint)
	 */
	public String getDropUniqueConstraintSQL(UniqueConstraint constraint)
	{
		return MessageFormat.format(this.dropUniqueConstraintFormat(), constraint.getName(), constraint.getTable());
	}

	/**
	 * @see net.sf.hajdbc.Dialect#isIdentity(net.sf.hajdbc.ColumnProperties)
	 */
	public boolean isIdentity(ColumnProperties properties)
	{
		String remarks = properties.getRemarks();
		
		return (remarks != null) && remarks.contains("GENERATED BY DEFAULT AS IDENTITY");
	}

	/**
	 * @see net.sf.hajdbc.Dialect#isSelectForUpdate(java.lang.String)
	 */
	public boolean isSelectForUpdate(String sql)
	{
		return this.selectForUpdatePattern.matcher(sql).find();
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#parseInsertTable(java.lang.String)
	 */
	public String parseInsertTable(String sql)
	{
		return this.parse(this.insertIntoTablePattern, sql);
	}

	/**
	 * @see net.sf.hajdbc.Dialect#getDefaultSchemas(java.sql.Connection)
	 */
	public List<String> getDefaultSchemas(Connection connection) throws SQLException
	{
		return Collections.singletonList(this.getCurrentUser(connection));
	}
	
	protected String getCurrentUser(Connection connection) throws SQLException
	{
		return this.executeFunction(connection, this.currentUserFunction());
	}
	
	protected String executeFunction(Connection connection, String function) throws SQLException
	{
		Statement statement = connection.createStatement();
		
		ResultSet resultSet = statement.executeQuery(this.executeFunctionSQL(function));
		
		resultSet.next();
		
		String value = resultSet.getString(1);
		
		resultSet.close();
		statement.close();
		
		return value;
	}
	
	protected String currentUserFunction()
	{
		return "CURRENT_USER";
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#parseSequence(java.lang.String)
	 */
	public String parseSequence(String sql)
	{
		return this.parse(this.sequencePattern, sql);
	}

	/**
	 * @see net.sf.hajdbc.Dialect#getColumnType(net.sf.hajdbc.ColumnProperties)
	 */
	public int getColumnType(ColumnProperties properties)
	{
		return properties.getType();
	}

	/**
	 * @see net.sf.hajdbc.Dialect#getSequences(java.sql.Connection)
	 */
	public Collection<String> getSequences(Connection connection) throws SQLException
	{
		List<String> sequenceList = new LinkedList<String>();
		
		ResultSet resultSet = connection.getMetaData().getTables("", null, "%", new String[] { this.sequenceTableType() });
		
		while (resultSet.next())
		{
			StringBuilder builder = new StringBuilder();
			
			String schema = resultSet.getString("TABLE_SCHEM");
			
			if (schema != null)
			{
				builder.append(schema).append(".");
			}
			
			sequenceList.add(builder.append(resultSet.getString("TABLE_NAME")).toString());
		}
		
		resultSet.close();
		
		return sequenceList;
	}

	protected String sequenceTableType()
	{
		return "SEQUENCE";
	}

	/**
	 * @see net.sf.hajdbc.Dialect#getCurrentSequenceValueSQL(java.lang.String)
	 */
	public String getCurrentSequenceValueSQL(String sequence)
	{
		return this.executeFunctionSQL(MessageFormat.format(this.currentSequenceValueFormat(), sequence));
	}
	
	/**
	 * @see net.sf.hajdbc.Dialect#getAlterSequenceSQL(java.lang.String, long)
	 */
	public String getAlterSequenceSQL(String sequence, long value)
	{
		return MessageFormat.format(this.alterSequenceFormat(), sequence, value);
	}

	/**
	 * @see net.sf.hajdbc.Dialect#supportsIdentityColumns()
	 */
	public boolean supportsIdentityColumns()
	{
		return true;
	}

	/**
	 * @see net.sf.hajdbc.Dialect#supportsSequences()
	 */
	public boolean supportsSequences()
	{
		return true;
	}

	protected String parse(Pattern pattern, String string)
	{
		Matcher matcher = pattern.matcher(string);
		
		return matcher.find() ? matcher.group(1) : null;
	}
	
	protected String truncateTableFormat()
	{
		return "DELETE FROM {0}";
	}
	
	protected String createForeignKeyConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} FOREIGN KEY ({2}) REFERENCES {3} ({4}) ON DELETE {5,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} ON UPDATE {6,choice,0#CASCADE|1#RESTRICT|2#SET NULL|3#NO ACTION|4#SET DEFAULT} {7,choice,5#DEFERRABLE INITIALLY DEFERRED|6#DEFERRABLE INITIALLY IMMEDIATE|7#NOT DEFERRABLE}";
	}
	
	protected String createUniqueConstraintFormat()
	{
		return "ALTER TABLE {1} ADD CONSTRAINT {0} UNIQUE ({2})";
	}
	
	protected String dropForeignKeyConstraintFormat()
	{
		return this.dropConstraintFormat();
	}
	
	protected String dropUniqueConstraintFormat()
	{
		return this.dropConstraintFormat();
	}
	
	protected String dropConstraintFormat()
	{
		return "ALTER TABLE {1} DROP CONSTRAINT {0}";
	}
	
	protected String selectForUpdatePattern()
	{
		return "SELECT\\s+.+\\s+FOR\\s+UPDATE";
	}

	protected String insertIntoTablePattern()
	{
		return "INSERT\\s+(?:INTO\\s+)?(\\S+)";
	}

	protected String sequencePattern()
	{
		return "NEXT\\s+VALUE\\s+FOR\\s+(\\S+)";
	}
	
	protected String alterSequenceFormat()
	{
		return "ALTER SEQUENCE {0} RESTART WITH {1}";
	}
	
	/**
	 * Although the SQL standard does not provide a mechanism for retrieving the current value of a sequence, if one did exist, it would probably look like this.
	 * @return a sql function pattern for retrieving the current value of a sequence.
	 */
	protected String currentSequenceValueFormat()
	{
		return "CURRENT VALUE FOR {0}";
	}
}