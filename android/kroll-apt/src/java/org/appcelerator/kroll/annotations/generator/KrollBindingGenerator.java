/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll.annotations.generator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class KrollBindingGenerator
{
	private static final String Kroll_DEFAULT = "org.appcelerator.kroll.annotations.Kroll.DEFAULT";

	private String outPath, moduleId;
	private Configuration fmConfig;
	private Template v8SourceTemplate, v8HeaderTemplate;
	private HashMap<String, Object> apiTree = new HashMap<String, Object>();
	private HashMap<String, Object> proxies = new HashMap<String, Object>();
	private HashMap<String, Object> modules = new HashMap<String, Object>();

	// These maps are used so we can load up Titanium JSON metadata when generating source for 3rd party modules
	private HashMap<String, Object> tiProxies = new HashMap<String, Object>();
	private HashMap<String, Object> tiModules = new HashMap<String, Object>();

	private JSONUtils jsonUtils;

	public KrollBindingGenerator(String outPath,  String moduleId)
	{
		this.outPath = outPath;
		this.moduleId = moduleId;

		this.jsonUtils = new JSONUtils();

		initTemplates();
	}

	protected void initTemplates()
	{
		fmConfig = new Configuration();
		fmConfig.setObjectWrapper(new DefaultObjectWrapper());
		fmConfig.setClassForTemplateLoading(getClass(), "");

		try {
			ClassLoader loader = getClass().getClassLoader();
			String templatePackage = "org/appcelerator/kroll/annotations/generator/";

			InputStream v8HeaderStream = loader.getResourceAsStream(templatePackage + "ProxyBindingV8.h.fm");
			InputStream v8SourceStream = loader.getResourceAsStream(templatePackage + "ProxyBindingV8.cpp.fm");

			v8HeaderTemplate = new Template("ProxyBindingV8.h.fm", new InputStreamReader(v8HeaderStream), fmConfig);

			v8SourceTemplate = new Template("ProxyBindingV8.cpp.fm", new InputStreamReader(v8SourceStream), fmConfig);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void saveTypeTemplate(Template template, String outFile, Map<Object, Object> root)
	{
		Writer writer = null;
		try {
			File file = new File(outPath, outFile);
			System.out.println("Generating " + file.getAbsolutePath());

			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}

			writer = new FileWriter(file);
			template.process(root, writer);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (writer != null) {
				try {
					writer.flush();
					writer.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	protected String getParentModuleClass(Map<String, Object> proxy)
	{
		String creatableInModule = (String) jsonUtils.getStringMap(proxy, "proxyAttrs").get("creatableInModule");
		String parentModule = (String) jsonUtils.getStringMap(proxy, "proxyAttrs").get("parentModule");

		if (creatableInModule != null && !creatableInModule.equals(Kroll_DEFAULT)) {
			return creatableInModule;

		} else if (parentModule != null && !parentModule.equals(Kroll_DEFAULT)) {
			return parentModule;
		}

		return null;
	}

	protected String getFullApiName(Map<String, Object> proxy)
	{
		String fullApiName = (String) jsonUtils.getStringMap(proxy, "proxyAttrs").get("name");
		String parentModuleClass = getParentModuleClass(proxy);

		while (parentModuleClass != null) {
			Map<String, Object> parent = jsonUtils.getStringMap(proxies, parentModuleClass);
			if (parent != null) {
			    Map<String, Object> proxyAttrs = jsonUtils.getStringMap(parent, "proxyAttrs");
			    if (proxyAttrs != null && (!proxyAttrs.containsKey("creatable") || (boolean) proxyAttrs.get("creatable"))) {
			        String parentName = (String) proxyAttrs.get("name");
	                fullApiName = parentName + "." + fullApiName;

			    }
			}
			
			parentModuleClass = getParentModuleClass(parent);
		}

		return fullApiName;
	}

	protected void addToApiTree(String className, Map<String, Object> proxy)
	{
		String fullApiName = getFullApiName(proxy);
		jsonUtils.getMap(proxy, "proxyAttrs").put("fullAPIName", fullApiName);

		Map<String, Object> tree = apiTree;
		String[] apiNames = fullApiName.split("\\.");
		for (String api : apiNames) {
			if (api.equals("Titanium")) {
				continue;
			}

			if (!tree.containsKey(api)) {
				HashMap<String, Object> subTree = new HashMap<String, Object>();
				tree.put(api, subTree);
			}

			tree = jsonUtils.getStringMap(tree, api);
		}
		tree.put("_className", className);
	}

	protected Map<String, Object> getProxyApiTree(Map<Object, Object> proxy)
	{
		String fullApiName = (String) jsonUtils.getMap(proxy, "proxyAttrs").get("fullAPIName");
		Map<String, Object> tree = apiTree;
		String[] apiNames = fullApiName.split("\\.");
		for (String api : apiNames) {
			if (api.equals("Titanium")) {
				continue;
			}
			tree = jsonUtils.getStringMap(tree, api);
		}
		return tree;
	}

	@SuppressWarnings("unchecked")
	private void mergeModules(Map<String, Object> source)
	{
		Set<String> newKeys = source.keySet();
		for (String key : newKeys) {
			Object newEntry = source.get(key);
			if (!modules.containsKey(key)) {
				modules.put(key, newEntry);
			} else {
				Object origEntry = modules.get(key);
				if (!(origEntry instanceof Map) || !(newEntry instanceof Map)) {
					// That would be odd indeed.
					continue;
				}

				Map<Object, Object> newEntryMap = (Map<Object, Object>) newEntry;
				Map<Object, Object> origEntryMap = (Map<Object, Object>) origEntry;

				if (newEntryMap.containsKey("apiName") && !origEntryMap.containsKey("apiName")) {
					origEntryMap.put("apiName", newEntryMap.get("apiName"));
				}

				String[] listNames = {"childModules", "createProxies"};
				for (String listName : listNames) {
					if (newEntryMap.containsKey(listName)) {
						JSONArray list = (JSONArray) newEntryMap.get(listName);
						for (int i = 0; i < list.size(); i++) {
							jsonUtils.appendUniqueObject(origEntryMap, listName, "id", (Map<Object, Object>) list.get(i));
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void loadBindings(String jsonPath)
		throws ParseException, IOException
	{
		FileReader reader = new FileReader(jsonPath);
		Map<String, Object> properties = (Map<String, Object>)
			JSONValue.parseWithException(reader);
		reader.close();

		Map<String, Object> proxies = jsonUtils.getStringMap(properties, "proxies");
		Map<String, Object> modules = jsonUtils.getStringMap(properties, "modules");

		this.proxies.putAll(proxies);
		mergeModules(modules);
	}

	@SuppressWarnings("unchecked")
	protected void loadTitaniumBindings()
		throws ParseException, IOException, URISyntaxException
	{
		// Load the binding JSON data from the titanium.jar relative to the kroll-apt.jar
		// where this class is defined in the MobileSDK

		// According to JavaDoc, getCodeSource() is the only possible "null" part of this chain
		CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
		if (codeSource == null) {
			System.err.println("Error: No code source found on the ClassLoader's protection domain");
			System.exit(1);
		}

		URL krollAptJarUrl = codeSource.getLocation();
		String mobileAndroidDir = new File(krollAptJarUrl.toURI()).getParent();

		JarFile titaniumJar = new JarFile(new File(mobileAndroidDir, "titanium.jar"));
		ZipEntry jsonEntry = titaniumJar.getEntry("org/appcelerator/titanium/bindings/titanium.json");
		InputStream jsonStream = titaniumJar.getInputStream(jsonEntry);

		Map<String, Object> properties = (Map<String, Object>)
			JSONValue.parseWithException(new InputStreamReader(jsonStream));
		jsonStream.close();
		titaniumJar.close();

		tiProxies.putAll(jsonUtils.getStringMap(properties, "proxies"));
		tiModules.putAll(jsonUtils.getStringMap(properties, "modules"));
	}
	
	@SuppressWarnings("unchecked")
    protected void loadTitaniumModuleBindings(final String module)
        throws ParseException, IOException, URISyntaxException
    {
        System.out.println("loadTitaniumModuleBindings " + module);
        // Load the binding JSON data from the titanium.jar relative to the kroll-apt.jar
        // where this class is defined in the MobileSDK

        // According to JavaDoc, getCodeSource() is the only possible "null" part of this chain
        CodeSource codeSource = getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            System.err.println("Error: No code source found on the ClassLoader's protection domain");
            System.exit(1);
        }

        URL krollAptJarUrl = codeSource.getLocation();
        String mobileAndroidDir = new File(krollAptJarUrl.toURI()).getParent();

        JarFile moduleJar = new JarFile(new File(mobileAndroidDir,  "modules/titanium-" + module + ".jar"));
        ZipEntry jsonEntry = moduleJar.getEntry("org/appcelerator/titanium/bindings/"+ module + ".json");
        if (jsonEntry == null) {
            moduleJar.close();
            return;
        }
        InputStream jsonStream = moduleJar.getInputStream(jsonEntry);

        Map<String, Object> properties = (Map<String, Object>)
            JSONValue.parseWithException(new InputStreamReader(jsonStream));
        jsonStream.close();
        moduleJar.close();

        tiProxies.putAll(jsonUtils.getStringMap(properties, "proxies"));
        tiModules.putAll(jsonUtils.getStringMap(properties, "modules"));
    }


	protected void generateApiTree()
	{
		// First pass generates the API tree
		for (String proxyName : proxies.keySet()) {
			Map<String, Object> proxy = jsonUtils.getStringMap(proxies, proxyName);
			addToApiTree(proxyName, proxy);
		}
	}

	protected void generateBindings()
		throws ParseException, IOException
	{
		for (String proxyName : proxies.keySet()) {
			Map<Object, Object> proxy = jsonUtils.getMap(proxies, proxyName);

			HashMap<Object, Object> root = new HashMap<Object, Object>(proxy);
			root.put("allModules", modules);
			root.put("allProxies", proxies);
			root.put("moduleId", moduleId);

			root.put("tiProxies", tiProxies);
			root.put("tiModules", tiModules);

			String v8ProxyHeader = proxyName + ".h";
			String v8ProxySource = proxyName + ".cpp";

			saveTypeTemplate(v8HeaderTemplate, v8ProxyHeader, root);
			saveTypeTemplate(v8SourceTemplate, v8ProxySource, root);

		}
	}

	public static void main(String[] args)
		throws Exception
	{
		if (args.length < 4) {
			System.err.println("Usage: KrollBindingGenerator <outdir> <isModule> <modulePackage> <binding.json> [<binding.json> ...]");
			System.exit(1);
		}

		String outDir = args[0];
		boolean isModule = "true".equalsIgnoreCase(args[1]);
        String packageName = args[2];
        String tiDeps = args[3];

		KrollBindingGenerator generator = new KrollBindingGenerator( outDir, packageName);
		
		
		
		
		// First pass to generate the entire API tree
		for (int i = 4; i < args.length; i++) {
			generator.loadBindings(args[i]);
		}

		if (isModule ) {
			generator.loadTitaniumBindings();
			//look for ti dependencies
            if (!"none".equals(tiDeps)) {
                String[] array = tiDeps.split(",");
                for (int i = 0; i < array.length; i++) {
                    generator.loadTitaniumModuleBindings(array[i]);
                }
            }
		}

		generator.generateApiTree();
		generator.generateBindings();

	}

}
