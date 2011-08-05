/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.webos.toolbar;

import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.webui.page.UIPage;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkingWorkspace;
import org.exoplatform.webos.webui.page.UIDesktopPage;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/webui/component/UIWebOSAdminToolbarPortlet.gtmpl")
public class UIWebOSAdminToolbarPortlet extends UIPortletApplication
{

   public UIWebOSAdminToolbarPortlet() throws Exception
   {
   }

   public UserNavigation getSelectedNavigation() throws Exception
   {
      return Util.getUIPortal().getUserNavigation();
   }

   @Override
   public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception
   {
      if (!isOnDesktop() && (hasEditPermissionOnNavigation() || hasEditPermissionOnPage() || hasEditPermissionOnPortal()))
      {
         super.processRender(app, context);
      }
   }

   private boolean isOnDesktop()
   {
      UIPortalApplication portalApp = Util.getUIPortalApplication();
      UIWorkingWorkspace uiWorkingWS = portalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
      UIPageBody pageBody = uiWorkingWS.findFirstComponentOfType(UIPageBody.class);
      UIPage uiPage = (UIPage)pageBody.getUIComponent();
      return uiPage != null && UIDesktopPage.DESKTOP_FACTORY_ID.equals(uiPage.getFactoryId());
   }

   private boolean hasEditPermissionOnNavigation() throws Exception
   {
      UserNavigation selectedNavigation = getSelectedNavigation();
      if (selectedNavigation == null)
      {
         return false;
      }
      
      return selectedNavigation.isModifiable();
   }
   
   private boolean hasEditPermissionOnPortal() throws Exception
   {
      UIPortalApplication portalApp = Util.getUIPortalApplication();
      UIPortal currentUIPortal = portalApp.<UIWorkingWorkspace>findComponentById(UIPortalApplication.UI_WORKING_WS_ID).findFirstComponentOfType(UIPortal.class);
      UserACL userACL = portalApp.getApplicationComponent(UserACL.class);
      return userACL.hasEditPermissionOnPortal(currentUIPortal.getSiteType().getName(), currentUIPortal.getSiteKey().getName(), currentUIPortal.getEditPermission());
   }

   private boolean hasEditPermissionOnPage() throws Exception
   {
      UIPortalApplication portalApp = Util.getUIPortalApplication();
      UIWorkingWorkspace uiWorkingWS = portalApp.getChildById(UIPortalApplication.UI_WORKING_WS_ID);
      UIPageBody pageBody = uiWorkingWS.findFirstComponentOfType(UIPageBody.class);
      UIPage uiPage = (UIPage)pageBody.getUIComponent();
      UserACL userACL = portalApp.getApplicationComponent(UserACL.class);

      if(uiPage != null)
      {
         return userACL.hasEditPermissionOnPage(uiPage.getOwnerType(), uiPage.getOwnerId(), uiPage.getEditPermission());
      }
      else
      {
         UIPortal currentUIPortal = portalApp.<UIWorkingWorkspace>findComponentById(UIPortalApplication.UI_WORKING_WS_ID).findFirstComponentOfType(UIPortal.class);
         UserNode currentNode = currentUIPortal.getSelectedUserNode();
         String pageReference = currentNode.getPageRef();
         if(pageReference == null)
         {
            return false;
         }
         else
         {
            DataStorage dataStorage = portalApp.getApplicationComponent(DataStorage.class);
            Page page = dataStorage.getPage(pageReference);
            return page != null && userACL.hasEditPermission(page);
         }
      }
   }
}
