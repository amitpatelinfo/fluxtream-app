package com.fluxtream.connectors.flickr;

import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import com.fluxtream.Configuration;
import com.fluxtream.auth.AuthHelper;
import com.fluxtream.connectors.Connector;
import com.fluxtream.connectors.controllers.ControllerSupport;
import com.fluxtream.domain.ApiKey;
import com.fluxtream.domain.Guest;
import com.fluxtream.domain.Notification;
import com.fluxtream.services.GuestService;
import com.fluxtream.services.NotificationsService;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.fluxtream.utils.HttpUtils.fetch;
import static com.fluxtream.utils.Utils.hash;

@Controller
@RequestMapping(value="/flickr")
public class FlickrController {

	@Autowired
	GuestService guestService;
	
	@Autowired
	Configuration env;

    @Autowired
    NotificationsService notificationsService;
	
	@RequestMapping(value = "/token")
	public String getToken(HttpServletRequest request)
		throws NoSuchAlgorithmException
	{
		String api_key = env.get("flickrConsumerKey");
		String api_sig = env.get("flickrConsumerSecret") + "api_key" + api_key + "permsread";
		api_sig = hash(api_sig);
        final String validRedirectUrl = env.get("flickr.validRedirectURL");
        if (!validRedirectUrl.startsWith(ControllerSupport.getLocationBase(request))) {
            final long guestId = AuthHelper.getGuestId();
            notificationsService.addNotification(guestId, Notification.Type.WARNING, "Flickr connector is only available for hosts that will respond to a redirect pointing to " + validRedirectUrl);
            return "redirect:/app";
        }
        String loginUrl = "http://flickr.com/services/auth/" +
			"?api_key=" + api_key + "&perms=read&api_sig=" + api_sig;
		return "redirect:" + loginUrl;
	}
	
	@RequestMapping(value = "/upgradeToken")
	public String upgradeToken(HttpServletRequest request) throws NoSuchAlgorithmException, IOException, DocumentException {
		String api_key = env.get("flickrConsumerKey");
		String frob = request.getParameter("frob");
		
		Map<String,String> params = new HashMap<String,String>();
		params.put("method", "flickr.auth.getToken");
		params.put("api_key", api_key);
		params.put("frob", frob);

        String api_sig = sign(params);
		
		String getTokenUrl = "http://api.flickr.com/services/rest/" +
			"?method=flickr.auth.getToken&api_key=" + api_key + "&frob=" + frob + "&api_sig=" + api_sig;

		Guest guest = AuthHelper.getGuest();

		String authToken = fetch(getTokenUrl);

        StringReader stringReader = new StringReader(authToken);
        StringBuilder sb = new StringBuilder();
        final List<String> responseLines = IOUtils.readLines(stringReader);
        sb.append("<root>");
        for (int i=1; i<responseLines.size(); i++)
            sb.append(responseLines.get(i));
        sb.append("</root>");

        SAXReader reader = new SAXReader();
        Document document = reader.read(new StringReader(authToken));

		Element user = (Element) document.selectSingleNode("rsp/auth/user");
		
		String username = user.attributeValue("username");

		String nsid = user.attributeValue("nsid");
		String fullname = user.attributeValue("fullname");
		String token = document.selectSingleNode("rsp/auth/token/text()").getStringValue();
		
		Connector flickrConnector = Connector.getConnector("flickr");

        final ApiKey apiKey = guestService.createApiKey(guest.getId(), flickrConnector);

		guestService.setApiKeyAttribute(apiKey,  "username", username);
		guestService.setApiKeyAttribute(apiKey,  "token", token);
		guestService.setApiKeyAttribute(apiKey,  "nsid", nsid);
		guestService.setApiKeyAttribute(apiKey,  "fullname", fullname);
		
		return "redirect:/app/from/"+flickrConnector.getName();
	}
	
	String sign(Map<String,String> parameters) throws NoSuchAlgorithmException {
		String toSign = env.get("flickrConsumerSecret");
	    SortedSet<String> eachKey= new TreeSet<String>(parameters.keySet());
		for (String key : eachKey)
			toSign += key + parameters.get(key);
		String sig = hash(toSign);
		return sig;
	}
	
	String getConsumerKey() {
		return env.get("flickrConsumerKey");
	}

	String getConsumerSecret() {
		return env.get("flickrConsumerSecret");
	}
	
}
