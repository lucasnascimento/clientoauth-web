package br.com.pearson.module.calls;
public class AbstractCall {
}
/*
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class AbstractCall {

	private static final String METHOD_PARAM = "X-METHOD";
	
	private RestTemplate restTemplate;

	protected String baseUrl;

	*//**
	 * C'tor<br>
	 * Inicializa o RestTemplate básico para suporte a forms e Strings
	 *//*
	public AbstractCall(String baseUrl) {

		RestTemplate restTemplate = new RestTemplate();

		HttpMessageConverter<MultiValueMap<String, ?>> formHttpMessageConverter = new FormHttpMessageConverter();
		HttpMessageConverter<String> stringHttpMessageConverternew = new StringHttpMessageConverter();
		HttpMessageConverter<byte[]> byteArrayMessageConverter = new ByteArrayHttpMessageConverter();
		HttpMessageConverter<BufferedImage> imageMessageConverter = new BufferedImageHttpMessageConverter();
		HttpMessageConverter<Resource> resourceMessageConverter = new ResourceHttpMessageConverter();

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(formHttpMessageConverter);
		converters.add(stringHttpMessageConverternew);
		converters.add(byteArrayMessageConverter);
		converters.add(imageMessageConverter);
		converters.add(resourceMessageConverter);

		restTemplate.setMessageConverters(converters);

		this.restTemplate = restTemplate;
		this.baseUrl = baseUrl;
	}

	*//**
	 * Executa o POST na URL especificada, com as varáveis que foram definidas
	 * 
	 * @param apiUri
	 *            a URI da chamada para a API
	 * @param variables
	 *            as varáveis da requisição
	 * @return a String que a API devolveu como resultado
	 *//*
	protected String doPost(String apiUri, Object requestVariables,
			Map<String, String> urlVariables)
			throws HttpClientErrorException {

		return this.restTemplate.postForObject(buildUrlWithVariables(apiUri, urlVariables),
				requestVariables, String.class, urlVariables);
	}

	*//**
	 * Executa o GET na URL especificada, com as varáveis que foram definidas
	 * 
	 * @param apiUri
	 *            a URI da API
	 * @param urlVariables
	 *            as varáveis enviadas na requisição
	 * @return o resultado da chamada
	 *//*
	protected String doGet(String apiUri, Map<String, String> urlVariables)
			throws HttpClientErrorException {

		return this.restTemplate.getForObject(buildUrlWithVariables(apiUri, urlVariables),
				String.class, urlVariables);
	}

	*//**
	 * Empty URL Variables
	 *//*
	protected Map<String, String> emptyURLVariables() {
		return new HashMap<String, String>();
	}

	*//**
	 * Executa um DELETE na URI passada
	 * 
	 * @param apiUri
	 *            o URI da chamada a API
	 *//*
	protected String doDelete(String apiUri, Object requestVariables,
			Map<String, String> urlVariables)
			throws HttpClientErrorException {

		HttpEntity<Object> entity = new HttpEntity<Object>(requestVariables, this.buildHeadersWithHiddenMethod(HttpMethod.DELETE));
		return this.restTemplate.exchange(buildUrlWithVariables(apiUri, urlVariables),
				HttpMethod.POST, entity, String.class, this.emptyURLVariables())
				.getBody();
	}

	*//**
	 * Executa um PUT na URI da API
	 * 
	 * @param apiUri
	 *            a URI
	 * @param urlVariables
	 *            as variáveis que serão enviadas no request
	 *//*
	protected String doPut(String apiUri, Object requestVariables,
			Map<String, String> urlVariables)
			throws HttpClientErrorException {

		HttpEntity<Object> entity = new HttpEntity<Object>(requestVariables, this.buildHeaders());
		return this.restTemplate.exchange(buildUrlWithVariables(apiUri, urlVariables),
				HttpMethod.PUT, entity, String.class, this.emptyURLVariables()).getBody();
	}
	
	private String buildUrlWithVariables(String apiUri, Map<String, String> urlVariables) {
		StringBuilder url = new StringBuilder().append(this.baseUrl).append(apiUri);
		if (urlVariables != null && urlVariables.size() > 0) {
			url.append('?');
			Iterator<Entry<String, String>> iterator = urlVariables.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String, ?> entry = iterator.next();
				url.append(entry.getKey());
				url.append("=");
				url.append(entry.getValue());
				url.append("&");
			}
		}
		return url.toString();
		
	}
	
	protected MultiValueMap<String,String> buildHeaders() {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
		headers.add("Content-Type", "application/json");
		headers.add("Accept", "/*");
		
		return headers;
	}
	
	protected MultiValueMap<String, String> buildHeadersWithHiddenMethod(HttpMethod method) {
		MultiValueMap<String, String> buildHeaders = this.buildHeaders();
		buildHeaders.add(METHOD_PARAM, method.toString());
		return buildHeaders;
	}
	
}
*/