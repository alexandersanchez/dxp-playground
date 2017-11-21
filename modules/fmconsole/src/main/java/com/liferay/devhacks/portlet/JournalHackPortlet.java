package com.liferay.devhacks.portlet;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.osgi.service.component.annotations.Component;

import com.liferay.devhacks.constants.JournalHackPortletKeys;
import com.liferay.devhacks.util.JournalHacksUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portal.kernel.io.unsync.UnsyncStringWriter;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.templateparser.TemplateNode;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * @author André Fabbro
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.display-name=FreeMarker Console Portlet",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + JournalHackPortletKeys.JournalHack,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user",
		"com.liferay.portlet.footer-portlet-javascript=/js/ace-editor/ace.js",
		"com.liferay.portlet.footer-portlet-javascript=/js/ace-editor/mode-ftl.js",
		"com.liferay.portlet.footer-portlet-javascript=/js/ace-editor/ext-language_tools.js"
	},
	service = Portlet.class
)
public class JournalHackPortlet extends MVCPortlet {
	
	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {

		String cmd = ParamUtil.getString(request, "cmd");

		if (Validator.equals(cmd, "renderTemplate")) {

			try {
				long scopeGroupId = PortalUtil.getScopeGroupId(request);

				String templateStr = ParamUtil.getString(request, "template");
				String articleId = ParamUtil.getString(request, "articleId");

				if (Validator.isNull(articleId)) {
					List<JournalArticle> head = JournalArticleLocalServiceUtil.getArticles(scopeGroupId, 0, 1);
					if (head == null || head.size() < 1) {
						JSONObject result = JSONFactoryUtil.createJSONObject();
						result.put("stat", "error");
						result.put("error", "No articles found");
						PortletResponseUtil.write(response, result.toString());
						return;
					}
					articleId = head.get(0).getArticleId();
				}
				Configuration c = new Configuration();

				c.setDefaultEncoding(StringPool.UTF8);
				c.setNumberFormat("0.######");

				Template template = new Template("templateName",
						new StringReader(templateStr), c);

				Map<String, Object> ctx = new HashMap<String, Object>();

				ctx.put("locale", request.getLocale());

				UnsyncStringWriter unsyncStringWriter = new UnsyncStringWriter();
				ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

				JournalHacksUtil.populateTokens(ctx,
						scopeGroupId, articleId, themeDisplay, request.getLocale().toLanguageTag().replace('-', '_'), request, response);

				Environment env = template.createProcessingEnvironment(ctx,
						unsyncStringWriter);

				env.process();

				JSONObject result = JSONFactoryUtil.createJSONObject();
				result.put("stat", "ok");
				result.put("result", unsyncStringWriter.toString());
				PortletResponseUtil.write(response, result.toString());

			} catch (Exception ex) {
				JSONObject result = JSONFactoryUtil.createJSONObject();
				result.put("stat", "error");
				result.put("error", ex.getLocalizedMessage());
				PortletResponseUtil.write(response, result.toString());
			}
		} else if (Validator.equals(cmd, "getVars")) {
			String articleId = ParamUtil.getString(request, "articleId");
			try {
				long groupId = PortalUtil.getScopeGroupId(request);
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);

				ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
				Document doc = SAXReaderUtil.read(article.getContent());
				List<TemplateNode> nodes = JournalHacksUtil.getTemplateNodes(themeDisplay, doc.getRootElement());

				JSONArray arr = JSONFactoryUtil.createJSONArray();
				for (TemplateNode node : nodes) {
					arr.put(node.getName());
				}
				JSONObject result = JSONFactoryUtil.createJSONObject();
				result.put("stat", "ok");
				result.put("result", arr.toString());
				PortletResponseUtil.write(response, result.toString());


			} catch (Exception ex) {
				JSONObject result = JSONFactoryUtil.createJSONObject();
				result.put("stat", "error");
				result.put("error", ex.getLocalizedMessage());
				PortletResponseUtil.write(response, result.toString());
			}

		} else {
			JSONObject result = JSONFactoryUtil.createJSONObject();
			result.put("stat", "error");
			result.put("error", "invalid cmd " + cmd);
			PortletResponseUtil.write(response, result.toString());
		}

	}
	
	
}