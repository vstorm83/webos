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

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserNodeFilterConfig;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webos.webui.page.UIDesktopPage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import java.util.Collection;
import java.util.Collections;

import javax.portlet.EventRequest;

/**
 * 
 * @author <a href="mailto:ndkhoi168@gmail.com">Nguyen Duc Khoi</a>
 */

@ComponentConfig(lifecycle = UIApplicationLifecycle.class, template = "app:/groovy/webui/component/UIUserToolBarDesktopPortlet.gtmpl",
   events =
      {
         @EventConfig(listeners = UIUserToolbarDesktopPortlet.CreateWebOSActionListener.class),
         @EventConfig(listeners = UIUserToolbarDesktopPortlet.NavigationChangeActionListener.class)
      }
)
public class UIUserToolbarDesktopPortlet extends UIPortletApplication
{
   public static String DEFAULT_TAB_NAME = "Tab_Default";

   private UserNodeFilterConfig toolbarFilterConfig;

   private UserNode desktopNode;

   public UIUserToolbarDesktopPortlet() throws Exception
   {
      UserNodeFilterConfig.Builder builder = UserNodeFilterConfig.builder();
      builder.withReadWriteCheck().withVisibility(Visibility.DISPLAYED, Visibility.TEMPORAL).withTemporalCheck();
      toolbarFilterConfig = builder.build();
   }

   public UserNavigation getCurrentUserNavigation() throws Exception
   {
      WebuiRequestContext rcontext = WebuiRequestContext.getCurrentInstance();
      return getNavigation(SiteKey.user(rcontext.getRemoteUser()));
   }

   private UserNavigation getNavigation(SiteKey userKey)
   {
      UserPortal userPortal = getUserPortal();
      return userPortal.getNavigation(userKey);
   }

   private UserPortal getUserPortal()
   {
      UIPortalApplication uiPortalApplication = Util.getUIPortalApplication();
      return uiPortalApplication.getUserPortalConfig().getUserPortal();
   }

   private UserNode getSelectedNode() throws Exception
   {
      return Util.getUIPortal().getSelectedUserNode();
   }

   private boolean isWebOSNode(UserNode userNode) throws Exception
   {
      if (userNode == null)
      {
         return false;
      }
      String pageRef = userNode.getPageRef();
      if (pageRef == null)
      {
         return false;
      }
      DataStorage ds = getApplicationComponent(DataStorage.class);
      Page page = ds.getPage(pageRef);
      return page == null || UIDesktopPage.DESKTOP_FACTORY_ID.equals(page.getFactoryId());
   }
   
   private UserNode findDashboardNode() throws Exception
   {
      Collection<UserNode> nodes = getUserNodes(getCurrentUserNavigation());
      if(nodes == null)
      {
         return null;
      }
      else
      {
         for(UserNode node : nodes)
         {
            if(!isWebOSNode(node))
            {
               return node;
            }
         }
         return null;
      }
   }

   private boolean isWebOSCreated() throws Exception
   {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      DataStorage storage = getApplicationComponent(DataStorage.class);
      Page page = storage.getPage(PortalConfig.USER_TYPE + "::" + context.getRemoteUser() + "::" + UIDesktopPage.PAGE_ID);
      return page != null;
   }

   static public class NavigationChangeActionListener extends EventListener<UIUserToolbarDesktopPortlet>
   {
      private Log log = ExoLogger.getExoLogger(NavigationChangeActionListener.class);

      @Override
      public void execute(Event<UIUserToolbarDesktopPortlet> event) throws Exception
      {
         log.debug("PageNode : " + ((EventRequest)event.getRequestContext().getRequest()).getEvent().getValue() + " is deleted");            
      }
   }


   /**
    * Create user page navigation, page and node for Desktop if they haven't been created already. 
    */
   static public class CreateWebOSActionListener extends EventListener<UIUserToolbarDesktopPortlet>
   {
      @Override
      public void execute(Event<UIUserToolbarDesktopPortlet> event) throws Exception
      {
         WebuiRequestContext context = event.getRequestContext();
         UIUserToolbarDesktopPortlet toolbarDesktopPortlet = event.getSource();
         String userName = context.getRemoteUser();

         if (userName != null)
         {
            Page page = createPage(userName, toolbarDesktopPortlet);
            toolbarDesktopPortlet.desktopNode = createNavigation(userName, page.getPageId(), toolbarDesktopPortlet);
            PortalRequestContext prContext = Util.getPortalRequestContext();
            NodeURL url = prContext.createURL(NodeURL.TYPE);
            url.setNode(toolbarDesktopPortlet.desktopNode);
            prContext.sendRedirect(url.toString());
         }
      }

      private Page createPage(String userName, UIUserToolbarDesktopPortlet toolbarDesktopPortlet) throws Exception
      {
         DataStorage service = toolbarDesktopPortlet.getApplicationComponent(DataStorage.class);
         Page page = service.getPage(PortalConfig.USER_TYPE + "::" + userName + "::" + UIDesktopPage.PAGE_ID);
         if (page == null)
         {
            page = new Page();
            page.setName(UIDesktopPage.PAGE_ID);
            page.setTitle(UIDesktopPage.PAGE_TITLE);
            page.setFactoryId(UIDesktopPage.DESKTOP_FACTORY_ID);
            page.setShowMaxWindow(true);
            page.setOwnerType(PortalConfig.USER_TYPE);
            page.setOwnerId(userName);
            service.create(page);
         }
         return page;
      }

      private UserNode createNavigation(String userName, String pageId, UIUserToolbarDesktopPortlet toolbarDesktopPortlet) throws Exception
      {
         UserPortal userPortal = toolbarDesktopPortlet.getUserPortal();
         UserNode rootNode = userPortal.getNode(toolbarDesktopPortlet.getCurrentUserNavigation(), Scope.CHILDREN,
               toolbarDesktopPortlet.toolbarFilterConfig, null);
         
         UserNode desktopNode = rootNode.getChild(UIDesktopPage.NODE_NAME);
         if (desktopNode == null)
         {
            desktopNode = rootNode.addChild(UIDesktopPage.NODE_NAME);
            desktopNode.setLabel(UIDesktopPage.NODE_LABEL);
            desktopNode.setPageRef(pageId);
            userPortal.saveNode(rootNode, null);
         }
         
         return desktopNode;
      }
   }

   public Collection<UserNode> getUserNodes(UserNavigation nav)
   {
      UserPortal userPortall = getUserPortal();
      if (nav != null)
      {
         try
         {
            UserNode rootNode = userPortall.getNode(nav, Scope.CHILDREN, toolbarFilterConfig, null);
            return rootNode.getChildren();
         }
         catch (Exception exp)
         {
            log.warn(nav.getKey().getName() + " has been deleted");
         }
      }
      return Collections.emptyList();
   }
}
