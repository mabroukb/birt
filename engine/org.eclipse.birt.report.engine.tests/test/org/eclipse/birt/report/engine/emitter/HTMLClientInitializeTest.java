/*******************************************************************************
 * Copyright (c) 2013 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.emitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.ReportEngine;

/**
 * Test case
 * 
 * 
 */
public class HTMLClientInitializeTest extends TestCase
{

	public void test( ) throws Exception
	{
		InputStream in = this.getClass( ).getResourceAsStream( "htmlClientInitializeTest.rptdesign" );
		EngineConfig config = new EngineConfig( );
		IReportEngine engine = new ReportEngine( config );
		IReportRunnable report = engine.openReportDesign( in );
		String clientInitialize = (String) report.getProperty( "clientInitialize" );
		IRunAndRenderTask task = engine.createRunAndRenderTask( report );
		// create the render options
		IRenderOption option = new HTMLRenderOption( );
		option.setOutputFormat( "html" ); //$NON-NLS-1$
		String htmlFileName = "test/org/eclipse/birt/report/engine/emitter/htmlClientInitializeTest.html";
		option.setOutputFileName( htmlFileName );
		// set the render options
		task.setRenderOption( option );
		task.run( );
		task.close( );
		engine.shutdown( );
		
		String[] jsArray = clientInitialize.split( "\\n" );		
		BufferedReader br = null;
		try
		{
			br = new BufferedReader( new FileReader( new File( htmlFileName ) ) );
			boolean isException = true;
			String str = br.readLine( ).trim( );
			int i = 0;
			int length = jsArray.length;
			while ( str != null )
			{
				boolean same = jsArray[i].trim( ).equals( str.trim( ) );
				if ( same )
				{
					if ( i == length - 1 )
					{
						isException = false;
						break;
					}
					i++;				
				} 
				str = br.readLine( );
			}
			assertFalse(isException);			
		}		
		finally
		{
			in.close( );
			br.close( );
			File file = new File(htmlFileName);			
			if ( file.exists( ) )
			{
				file.delete( );				
			}
		
		}
	}
}