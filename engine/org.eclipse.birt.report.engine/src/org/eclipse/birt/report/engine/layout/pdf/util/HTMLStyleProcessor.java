/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/

package org.eclipse.birt.report.engine.layout.pdf.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.birt.report.engine.content.IStyle;
import org.eclipse.birt.report.engine.css.dom.StyleDeclaration;
import org.eclipse.birt.report.engine.css.engine.BIRTCSSEngine;
import org.eclipse.birt.report.engine.css.engine.CSSEngine;
import org.eclipse.birt.report.engine.css.engine.StyleConstants;
import org.eclipse.birt.report.engine.css.engine.value.URIValue;
import org.eclipse.birt.report.engine.ir.DimensionType;
import org.eclipse.birt.report.engine.util.FileUtil;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.css.CSSValue;

/**
 * Converts the deprecated element according to the HTML 4.0 specification and
 * parses the style attribute of the HTML element.
 * 
 */
public class HTMLStyleProcessor
{

	/** the logger */
	private static Logger logger = Logger.getLogger( HTMLStyleProcessor.class
			.getName( ) );

	private ReportDesignHandle report;

	/** the CSS2.0 Parser */
	private CSSEngine cssEngine;

	private Pattern pattern;

	/**
	 * Constructor
	 * 
	 * @param context
	 *            the execution context
	 */
	public HTMLStyleProcessor( ReportDesignHandle report )
	{
		// Takes the zero-length string as parameter just for keeping to the
		// interface of constructor
		cssEngine = new BIRTCSSEngine( );
		this.report = report;
		pattern = Pattern.compile( "[ ]*([^:]*)[ ]*:[ ]*([^;]*)[ ]*[;]*" );
	}

	protected void processBackgroundImage( IStyle style, Map context )
	{
		if ( style != null )
		{
			CSSValue value = (CSSValue) style
					.getProperty( StyleConstants.STYLE_BACKGROUND_IMAGE );
			if ( value != null && value instanceof URIValue )
			{
				String bgi = ( (URIValue) value ).getStringValue( );
				if ( ( null != bgi ) && ( !"".equals( bgi ) ) ) //$NON-NLS-1$
				{
					if ( report != null )
					{
						if ( FileUtil.isLocalResource( bgi ) )
						{
							URL url = report.findResource( bgi,
									IResourceLocator.IMAGE, context );
							if ( url != null )
							{
								String fileName = url.getFile( );
								if ( fileName != null )
								{
									bgi = fileName;
								}
							}
						}
						else
						{
							bgi = "url(" + bgi + ")"; //$NON-NLS-1$//$NON-NLS-2$
						}
					}
					if ( bgi != null )
					{
						// Puts the modified URI of the resource
						style.setBackgroundImage( bgi );
					}
					else
					{
						// If the resource does not exist, then removes this
						// item.
						style.removeProperty( "background-image" ); //$NON-NLS-1$
					}
				}
			}
		}
	}

	protected StyleProperties getStyleProperties( Element ele,
			HashMap<Element, StyleProperties> styles )
	{
		StyleProperties sp = styles.get( ele );
		if ( sp == null )
		{
			sp = new StyleProperties( new StyleDeclaration( cssEngine ) );
			styles.put( ele, sp );
		}
		return sp;
	}

	/**
	 * Parses the style attribute of the element node and converts the
	 * deprecated element node in HTML 4.0, and calls it on its children element
	 * nodes recursively
	 * 
	 * @param ele
	 *            the element node in the DOM tree
	 * @param text
	 *            the text content object
	 */
	public void execute( Element ele, HashMap<Element, StyleProperties> styles,
			Map context )
	{

		StyleDeclaration style = null;
		StringBuffer strStyle = new StringBuffer( );
		StyleProperties sp = getStyleProperties( ele, styles );
		try
		{
			String inlineStyle = ele.getAttribute( "style" ); //$NON-NLS-1$
			if ( null != inlineStyle && !"".equals( inlineStyle ) ) //$NON-NLS-1$
			{
				StringBuffer buffer = new StringBuffer( );
				Matcher matcher = pattern.matcher( inlineStyle );
				while ( matcher.find( ) )
				{
					String name = matcher.group( 1 );
					String value = matcher.group( 2 );
					if ( name != null && name.length( ) > 0 && value != null
							&& value.length( ) > 0 )
					{
						ShortHandProcessor.process( buffer, name, value,
								cssEngine );
					}
					if ( StyleProperties.WIDTH.equals( name ) )
					{
						if ( value != null && value.length( ) > 0 )
						{
							DimensionType d = DimensionType.parserUnit( value );
							if ( d != null )
							{
								sp.addProperty( StyleProperties.WIDTH, d );
							}
						}
					}
					if ( StyleProperties.HEIGHT.equals( name ) )
					{
						if ( value != null && value.length( ) > 0 )
						{
							DimensionType d = DimensionType.parserUnit( value );
							if ( d != null )
							{
								sp.addProperty( StyleProperties.HEIGHT, d );
							}
						}
					}
				}
				strStyle.append( buffer.toString( ) );
			}
			if ( strStyle.length( ) > 0 )
			{
				sp.getStyle( ).setProperties(
						(StyleDeclaration) cssEngine
								.parseStyleDeclaration( strStyle.toString( ) ) );
			}
		}
		catch ( Exception e )
		{
			logger.log( Level.SEVERE, "The css statement is:" //$NON-NLS-1$
					+ ele.getAttribute( "style" ), e ); //$NON-NLS-1$
		}
		ele.removeAttribute( "style" ); //$NON-NLS-1$

		// handle background image
		processBackgroundImage( style, context );

		// Walks on its children nodes recursively
		for ( int i = 0; i < ele.getChildNodes( ).getLength( ); i++ )
		{
			Node child = ele.getChildNodes( ).item( i );
			if ( child.getNodeType( ) == Node.ELEMENT_NODE )
			{
				execute( (Element) child, styles, context );
			}
		}
	}

	/**
	 * Replaces the previous element with the new tag name in the same position
	 * and return it
	 * 
	 * @param oldEle
	 *            the replaced element
	 * @param tag
	 *            the tag name of the new HTML element
	 * @return the new HTML element
	 */
	private Element replaceElement( Element oldEle, String tag )
	{
		Element newEle = oldEle.getOwnerDocument( ).createElement( tag );
		// Copies the attributes
		for ( int i = 0; i < oldEle.getAttributes( ).getLength( ); i++ )
		{
			String attrName = oldEle.getAttributes( ).item( i ).getNodeName( );
			newEle.setAttribute( attrName, oldEle.getAttribute( attrName ) );
		}
		// Copies the children nodes
		// Note: After the child node is moved to another parent node, then
		// relationship between it and its sibling is removed. So here calls
		// <code>Node.getFirstChild()</code>again and again till it is null.
		for ( Node child = oldEle.getFirstChild( ); child != null; child = oldEle
				.getFirstChild( ) )
		{
			newEle.appendChild( child );
		}
		oldEle.getParentNode( ).replaceChild( newEle, oldEle );
		return newEle;
	}

	private void appendStyle( StringBuffer style, String name, String value )
	{
		if ( name == null
				|| "".equals( name ) || value == null || "".equals( value ) ) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return;
		}
		style.append( name + ":" + value + ";" ); //$NON-NLS-1$//$NON-NLS-2$
	}

}
