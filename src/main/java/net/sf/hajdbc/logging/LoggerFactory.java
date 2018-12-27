/*
 * HA-JDBC: High-Availability JDBC
 * Copyright (C) 2012  Paul Ferraro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.hajdbc.logging;

import java.util.Map;
import java.util.ServiceLoader;

import net.sf.hajdbc.logging.slf4j.SLF4JLoggingProvider;

/**
 * Factory for creating {@link Logger} implementation from various logging service provider implementations.
 * @author Paul Ferraro
 */
public final class LoggerFactory
{
	private static final LoggingProvider provider = getProvider();

	private static String getPrefLoggerClassName(){
		String ret = "";
		Map<String, String> env = null;
		try {
			env = System.getenv();
			String prefLoggerClassName = env.get("ICT_HAJDBC_PREF_LOGGER_CLASSNAME");

			if (prefLoggerClassName != null && !prefLoggerClassName.equals("")){
				ret = prefLoggerClassName;
			}
		}catch(Exception e){ 
			/// something wrong, e.g. 'SecurityException' depending of security manager configuration
			ret = "";
		}
		return ret;
	}

	private static LoggingProvider getProvider()
	{
		String prefLogger = getPrefLoggerClassName();
		if (prefLogger != null && !prefLogger.equals("")){
			LoggingProvider providerPref = null;
			System.out.println("ICT_debug: Env var ICT_HAJDBC_PREF_LOGGER_CLASSNAME = " + prefLogger);
			if (prefLogger.equals("net.sf.hajdbc.logging.jboss.JBossLoggingProvider")){
				providerPref = new net.sf.hajdbc.logging.jboss.JBossLoggingProvider();
			}
			else 
			if (prefLogger.equals("net.sf.hajdbc.logging.slf4j.SLF4JLoggingProvider")){
				providerPref = new net.sf.hajdbc.logging.slf4j.SLF4JLoggingProvider();
			}
			else 
			if (prefLogger.equals("net.sf.hajdbc.logging.commons.CommonsLoggingProvider")){
				providerPref = new net.sf.hajdbc.logging.commons.CommonsLoggingProvider();
			}
			else 
			if (prefLogger.equals("net.sf.hajdbc.logging.jdk.JDKLoggingProvider")){
				providerPref = new net.sf.hajdbc.logging.jdk.JDKLoggingProvider();
			}
			if (providerPref != null){
				System.out.println("ICT_debug: Using preferred logger: " + providerPref.getName());
				return providerPref;
			}
		}

		for (LoggingProvider provider: ServiceLoader.load(LoggingProvider.class, LoggingProvider.class.getClassLoader()))
		{
			if (provider.isEnabled())
			{
				provider.getLogger(LoggerFactory.class).log(Level.DEBUG, "Using {0} logging", provider.getName());
				
				System.err.println("ICT_debug: Using " + provider.getName() + " logging");
				return provider;
			}
		}
		throw new IllegalStateException(String.format("No %s found", LoggingProvider.class.getName()));
	}
	
	public static Logger getLogger(Class<?> targetClass)
	{
		return provider.getLogger(targetClass);
	}
	
	private LoggerFactory()
	{
		// Hide
	}
}
