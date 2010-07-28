/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/containerpage/client/ui/Attic/CmsToolbarContextButton.java,v $
 * Date   : $Date: 2010/07/21 11:02:34 $
 * Version: $Revision: 1.4 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.ade.containerpage.client.ui;

import org.opencms.ade.containerpage.client.CmsContainerpageHandler;
import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.ui.CmsContextMenu;
import org.opencms.gwt.client.ui.CmsContextMenuHandler;
import org.opencms.gwt.client.ui.I_CmsButton;
import org.opencms.gwt.client.ui.I_CmsContextMenuEntry;
import org.opencms.gwt.client.ui.css.I_CmsLayoutBundle;
import org.opencms.gwt.client.ui.input.CmsLabel;
import org.opencms.gwt.client.util.CmsCollectionUtil;
import org.opencms.gwt.shared.CmsCoreData.AdeContext;

import java.util.List;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;

/**
 * The context tool-bar menu.<p>
 * 
 * @author Ruediger Kurz
 * 
 * @version $Revision: 1.4 $
 * 
 * @since 8.0.0
 */
public class CmsToolbarContextButton extends A_CmsToolbarMenu {

    /** Signals whether the widget has been initialized or not. */
    private boolean m_initialized;

    /** The main content widget. */
    private FlexTable m_menuPanel;

    /** The handler resize registration. */
    private HandlerRegistration m_resizeRegistration;

    /**
     * Constructor.<p>
     * 
     * @param handler the container-page handler
     */
    public CmsToolbarContextButton(CmsContainerpageHandler handler) {

        super(I_CmsButton.ButtonData.CONTEXT, handler);

        // create the menu panel (it's a table because of ie6)
        m_menuPanel = new FlexTable();
        // set a style name for the menu table
        m_menuPanel.getElement().addClassName(I_CmsLayoutBundle.INSTANCE.contextmenuCss().menuPanel());

        // set the widget
        setMenuWidget(m_menuPanel);

        // remove the style attribute of the popup because its width is set to 100%
        DOM.removeElementAttribute(getPopupContent().getWidget().getElement(), "style");

    }

    /**
     * @see org.opencms.ade.containerpage.client.ui.I_CmsToolbarButton#onToolbarActivate()
     */
    public void onToolbarActivate() {

        if (!m_initialized) {
            getHandler().loadContextMenu(CmsCoreProvider.get().getUri(), AdeContext.containerpage);
            m_initialized = true;
        }
    }

    /**
     * Unregister the resize handler.<p>
     * 
     * @see org.opencms.ade.containerpage.client.ui.I_CmsToolbarButton#onToolbarDeactivate()
     */
    public void onToolbarDeactivate() {

        if (m_resizeRegistration != null) {
            m_resizeRegistration.removeHandler();
            m_resizeRegistration = null;
        }
    }

    /**
     * Creates the menu and adds it to the panel.<p>
     * 
     * @param menuEntries the menu entries 
     */
    public void showMenu(List<I_CmsContextMenuEntry> menuEntries) {

        if (!CmsCollectionUtil.isEmptyOrNull(menuEntries)) {
            // if there were entries found for the menu, create the menu
            CmsContextMenu menu = new CmsContextMenu(menuEntries, true);
            // add the resize handler for the menu
            m_resizeRegistration = Window.addResizeHandler(menu);
            // set the menu as widget for the panel 
            m_menuPanel.setWidget(0, 0, menu);
            // add the close handler for the menu
            getPopupContent().addCloseHandler(new CmsContextMenuHandler(menu));
        } else {
            // if no entries were found, inform the user 
            CmsLabel label = new CmsLabel("No entries found for this resource type!");
            label.addStyleName(I_CmsLayoutBundle.INSTANCE.contextmenuCss().menuInfoLabel());
            m_menuPanel.setWidget(0, 0, label);
        }
    }
}