package com.dotcms.xmlsitemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.business.FileFactory;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.XMLUtils;
import com.liferay.portal.model.User;

/**
 * This class manage the generation of the XMLSitemap<X>.xml.gz files from every
 * host in this dotCMS site
 *
 * @author Oswaldo
 *
 */
public class XMLSitemapJob implements Job, StatefulJob {

	private Host currentHost = null;
	private User systemUser = null;
	private File temporaryFile = null;
	private File compressedFile = null;
	private OutputStreamWriter out = null;

	private int sitemapCounter = 1;
	private int processedRegistries = 0;
	private Map<String, Integer> hostFilesCounter = null;

	private static String XML_SITEMAPS_FOLDER;
	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private FileAPI fileAPI = APILocator.getFileAPI();
	private FileFactory ffac = FactoryLocator.getFileFactory();
	private FolderAPI folderAPI = APILocator.getFolderAPI();
	private HTMLPageAPI htmlAPI = APILocator.getHTMLPageAPI();
	private IdentifierAPI identAPI = APILocator.getIdentifierAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private HostAPI hostAPI = APILocator.getHostAPI();
	private FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private boolean usePermalinks = false;
	private boolean useStructureURLMap = true;
	private String changeFrequencyConfig = "daily";
	private String modifiedDateStringValue = UtilMethods.dateToHTMLDate(
			new java.util.Date(), "yyyy-MM-dd");

	private String structuresToIgnoreConfig = null;

	public XMLSitemapJob() {
		try {
			systemUser = userAPI.getSystemUser();

			hostFilesCounter = new HashMap<String, Integer>();

			XML_SITEMAPS_FOLDER = Config.getStringProperty("org.dotcms.XMLSitemap.XML_SITEMAPS_FOLDER","/XMLSitemaps/");

			String usePermalinksString = Config.getStringProperty("org.dotcms.XMLSitemap.USE_PERMALINKS=false","false");

			String useStructureURLMapString = Config.getStringProperty("org.dotcms.XMLSitemap.USE_STRUCTURE_URL_MAP","false");

			usePermalinks = (UtilMethods.isSet(usePermalinksString) ? Boolean
					.parseBoolean(usePermalinksString) : false);

			useStructureURLMap = (UtilMethods.isSet(useStructureURLMapString) ? Boolean
					.parseBoolean(useStructureURLMapString)
					: false);

			modifiedDateStringValue = UtilMethods.dateToHTMLDate(
					new java.util.Date(System.currentTimeMillis()),
					"yyyy-MM-dd");

			structuresToIgnoreConfig = Config.getStringProperty("org.dotcms.XMLSitemap.IGNORE_Structure_Ids","");

			//generateSitemapPerHost();
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			systemUser = userAPI.getSystemUser();
			generateSitemapPerHost();

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}

	}

	private List<String> getIgnorableStrcutureIdsForHost(Host currentHost) {
		List<String> ignorableStructureIds = new ArrayList<String>();
		if (UtilMethods.isSet(structuresToIgnoreConfig)) {
			String[] ignorableStructureNames = structuresToIgnoreConfig
					.split(",");
			for (String ignorableStructureName : ignorableStructureNames) {
				ignorableStructureIds.add(ignorableStructureName.toLowerCase());
			}
		}
		return ignorableStructureIds;
	}

	private String getUrlPatternReplacementText(Host currentHost,
			String structureName) {
		String replacingText = "";

		try {
			String replacementTextFromConfig = Config.getStringProperty("org.dotcms.XMLSitemap." + structureName
							+ ".IGNORE_UrlText","");

			if (UtilMethods.isSet(replacementTextFromConfig)) {
				replacingText = replacementTextFromConfig;
			}
		} catch (Exception exception) {
			// ignore this error and return the default text.
		}
		return replacingText;
	}

	/**
	 * Generate the sitemap xml based on the show on menu pages, files, link and
	 * folder
	 */
	@SuppressWarnings("unchecked")
	public void generateSitemapPerHost() throws DotDataException,
			DotSecurityException {

		List<Host> hostsList = hostAPI.findAll(systemUser, false);

		int orderDirection = 1;

		List<String> structureVelocityVarNames = StructureFactory
				.getAllVelocityVariablesNames();

		for (Host host : hostsList) {

			if(host.isSystemHost())
				continue;

			processedRegistries = 0;
			currentHost = host;
			sitemapCounter = 1;
			String stringbuf = null;

			try {
				/**
				 * remove all the existing sitemaps generated in the XMLSitemap
				 * folder for the host specified
				 */
				cleanHostFromSitemapFiles(host);

				hostFilesCounter.put(host.getHostname(), sitemapCounter);

				/* adding host url */
				stringbuf = "<url><loc>"
						+ XMLUtils.xmlEscape("http://www." + host.getHostname()
								+ "/") + "</loc><lastmod>"
						+ modifiedDateStringValue
						+ "</lastmod><changefreq>daily</changefreq></url>\n";

				writeFile(stringbuf);

				addRegistryProcessed();

				List<String> ignorableStructureIds = getIgnorableStrcutureIdsForHost(host);
				/**
				 * This part generate the detail pages sitemap links per
				 * structure
				 */
				for (String stVelocityVarName : structureVelocityVarNames) {

					if (ignorableStructureIds.contains(stVelocityVarName
							.toLowerCase()))
						continue;

					Structure st = StructureFactory
							.getStructureByVelocityVarName(stVelocityVarName);

					if (!InodeUtils.isSet(st.getPagedetail()))
						continue;

					HTMLPage page = htmlAPI.loadLivePageById(
							st.getPagedetail(), systemUser, true);

					Logger.debug(this, " Creating Site Map for Structure "
							+ stVelocityVarName);
					Identifier pageIdentifier = identAPI.find(page
							.getIdentifier());

					Logger.debug(this,
							" Performing Host Parameter validation Page Identifider Host ["
									+ pageIdentifier.getHostId()
									+ "], Host Identifider ["
									+ host.getIdentifier() + "], Deleted ["
									+ page.isDeleted() + "], Live ["
									+ page.isLive() + "]");

					if (!(host.getIdentifier().equals(
							pageIdentifier.getHostId()) && (!page.isDeleted() && page
							.isLive()))) {
						Logger.debug(this,
								"Host Parameter validation failed for structure ["
										+ stVelocityVarName + "]");
						continue;
					}

					String query = "+structureName:" + st.getVelocityVarName()
							+ " +deleted:false +live:true";

					List<Contentlet> hits = conAPI.search(query, -1, 0, "",
							systemUser, true);

					String structureURLMap = st.getUrlMapPattern();

					List<RegExMatch> matches = null;

					if (useStructureURLMap
							&& UtilMethods.isSet(structureURLMap)) {
						matches = RegEX.find(st.getUrlMapPattern(),
								"({[^{}]+})");
					}
					for (Contentlet contenlet : hits) {
						try {
							if (usePermalinks) {
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://www."
												+ host.getHostname()
												+ "/permalink/"
												+ contenlet.getIdentifier()
												+ "/" + st.getPagedetail()
												+ "/")
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
							} else if (useStructureURLMap
									&& UtilMethods.isSet(structureURLMap)
									&& (matches != null)) {
								String uri = structureURLMap;
								Logger.debug(this,
										" Found the URL String for validation ["
												+ uri + "]");
								for (RegExMatch match : matches) {
									String urlMapField = match.getMatch();
									String urlMapFieldValue = contenlet
											.getStringProperty(urlMapField
													.substring(1, (urlMapField
															.length() - 1)));
									urlMapField = urlMapField.replaceFirst(
											"\\{", "\\\\{");
									urlMapField = urlMapField.replaceFirst(
											"\\}", "\\\\}");

									if (urlMapFieldValue != null) {
										uri = uri.replaceAll(urlMapField,
												urlMapFieldValue);
									}
									Logger.debug(this,
											"Performing Variable replacement - urlMapField ["
													+ match.getMatch()
													+ "], urlMapField [ "
													+ urlMapField
													+ "], urlMapFieldValue ["
													+ urlMapFieldValue
													+ "], uri [" + uri + "]");
								}

								if (uri == null
										&& UtilMethods
												.isSet(st.getDetailPage())) {
									if (page != null
											&& UtilMethods.isSet(page
													.getIdentifier())) {
										uri = page.getURI() + "?id="
												+ contenlet.getInode();
									}
								}
								String urlRelacementText = getUrlPatternReplacementText(
										host, stVelocityVarName);

								uri = uri.replaceAll(urlRelacementText, "");

								Logger.debug(this,
										"Performing URL replacement - urlRelacementText ["
												+ urlRelacementText
												+ "], uri [" + uri + "]");

								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://www."
												+ host.getHostname() + uri)
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
							} else {
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://www."
												+ host.getHostname()
												+ pageIdentifier.getURI()
												+ "?id="
												+ contenlet.getIdentifier())
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
							}
							writeFile(stringbuf);
							addRegistryProcessed();

						} catch (Exception e) {
							Logger.error(this, e.getMessage(), e);
						}
					}
				}
				/**
				 * This part add the show on menu pages. similar as we do in
				 * navigationwebapi to generate sitemap
				 * */
				java.util.List<Folder> itemsList = new ArrayList<Folder>();

				itemsList = folderAPI.findSubFolders(host, true);

				//Logger.warn(this, "Finding Subfolders for referebce [" + itemsList.size() + "]");

				Comparator<Folder> comparator = new AssetsComparator(
						orderDirection);
				Collections.sort(itemsList, comparator);

				List<Inode> itemsList2 = new ArrayList<Inode>();

				for (Folder f : itemsList) {
					if (f instanceof Folder) {
						//Logger.warn(this, "Folder Iteration in progress Name [" + f.getName() + "], show on Menu Indicator [" + f.isShowOnMenu() + "]");
						itemsList2.addAll(folderAPI.findMenuItems(f,
								systemUser, true));
					}
				}

				//Logger.warn(this, "List Size [" + itemsList2 + "]");

				if (itemsList2.size() > 0) {

					// /FIRST LEVEL MENU ITEMS!!!!
					for (Permissionable itemChild : itemsList) {

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;

							Logger.warn(this, "Folder Iteration in progress Name [" + folderChild.getName() + "], show on Menu Indicator [" + folderChild.isShowOnMenu() + "]");

							// recursive method here
							buildSubFolderSiteMapMenu(folderChild, 100, 1, 1);

						} else if (itemChild instanceof Link) {
							Link link = (Link) itemChild;
							if (link.isLive() && !link.isDeleted()) {
								if (link.getUrl()
										.startsWith(host.getHostname())) {
									stringbuf = "<url><loc>"
											+ XMLUtils.xmlEscape(link
													.getProtocal()
													+ link.getUrl())
											+ "</loc><lastmod>"
											+ modifiedDateStringValue
											+ "</lastmod><changefreq>daily</changefreq></url>\n";
									writeFile(stringbuf);
									addRegistryProcessed();
								}
							}
						} else if (itemChild instanceof HTMLPage) {
							HTMLPage page = (HTMLPage) itemChild;
							Logger.warn(this, "Folder Page Configuration " + page.getURI());
							if (page.isLive() && !page.isDeleted()) {
								String indexPageConfiguration = "/index."+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
								String pathToPageUrl = XMLUtils.xmlEscape("http://www."+ host.getHostname() + page.getURI());

								if (pathToPageUrl.endsWith(indexPageConfiguration)) {
									pathToPageUrl = pathToPageUrl.replace(indexPageConfiguration, "");
								}

								stringbuf = "<url><loc>"
										+ pathToPageUrl
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
								writeFile(stringbuf);
								addRegistryProcessed();
							}
						} else if (itemChild instanceof com.dotmarketing.portlets.files.model.File) {
							com.dotmarketing.portlets.files.model.File file = (com.dotmarketing.portlets.files.model.File) itemChild;
							if (file.isLive() && !file.isDeleted()) {
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://www."
												+ host.getHostname()
												+ file.getURI())
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
								writeFile(stringbuf);
								addRegistryProcessed();
							}
						} else if (itemChild instanceof Contentlet) {
							Contentlet fileContent = (Contentlet)itemChild;
							if (fileContent.isLive() && !fileContent.isArchived()) {
								Identifier identifier = APILocator.getIdentifierAPI().find(fileContent);
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://www."
												+ host.getHostname()
												+ UtilMethods.encodeURIComponent(identifier.getParentPath()+fileContent.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)))
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
								writeFile(stringbuf);
								addRegistryProcessed();
							}
						}
					}

				}

			} catch (Exception e) {
				Logger.error(this, e.getMessage(), e);
			}

			if (UtilMethods.isSet(temporaryFile)) {
				closeFileWriter();
			}
		}
	}

	/**
	 * Add the subfolder site map code to the xml site map file
	 *
	 * @param stringbuf
	 *            StringBuffer.
	 * @param thisFolder
	 *            Folder.
	 * @param numberOfLevels
	 *            int.
	 * @param currentLevel
	 *            int.
	 * @param orderDirection
	 *            int.
	 * @param menuIdPrefix
	 *            String.
	 * @return StringBuffer
	 */
	@SuppressWarnings("unchecked")
	private void buildSubFolderSiteMapMenu(Folder thisFolder,
			int numberOfLevels, int currentLevel, int orderDirection)
			throws DotDataException, DotSecurityException {

		String stringbuf = null;
		// gets menu items for this folder
		java.util.List<Inode> itemsChildrenList2 = folderAPI.findMenuItems(
				thisFolder, orderDirection);

		Identifier folderIdent = identAPI.find(thisFolder.getIdentifier());

		String folderChildPath = folderIdent.getURI().substring(0,
				folderIdent.getURI().length() - 1);

		folderChildPath = folderChildPath.substring(0, folderChildPath
				.lastIndexOf("/"));

		Host host = hostAPI.findParentHost(thisFolder, systemUser, false);

		Identifier id = identAPI.loadFromCache(host, folderIdent.getURI()
				+ "/index."
				+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));

		Logger.warn(this, "Performing check for folders [" + (folderIdent.getURI() + "/index." + Config.getStringProperty("VELOCITY_PAGE_EXTENSION")) + "], Identifier Check ["
		 + (id != null) + "], Children Count [" + itemsChildrenList2.size() + "], Host Identifier [" + host.getIdentifier() + "], Identifier " +
		 ((id != null) ? id.getInode() : "") + "]");

		boolean isIndexPageAlreadyConfigured = false;

		if ((id != null) && InodeUtils.isSet(id.getInode())) {
			stringbuf = "<url><loc>"
					+ XMLUtils
							.xmlEscape("http://www."
									+ host.getHostname()
									+ folderIdent.getURI())
					+ "</loc><lastmod>" + modifiedDateStringValue
					+ "</lastmod><changefreq>daily</changefreq></url>\n";

			Logger.warn(this, "Writing the XMLConfiguration for Folder[" + XMLUtils
							.xmlEscape("http://www."
									+ host.getHostname()
									+ folderIdent.getURI()) + "]"
			);

			isIndexPageAlreadyConfigured = true;

			writeFile(stringbuf);
			addRegistryProcessed();
		}

		if (currentLevel < numberOfLevels) {

			for (Permissionable childChild2 : itemsChildrenList2) {
				if (childChild2 instanceof Folder) {
					Folder folderChildChild2 = (Folder) childChild2;
					Identifier childChild2Ident = identAPI
							.find(folderChildChild2.getIdentifier());

					if (currentLevel <= numberOfLevels) {
						buildSubFolderSiteMapMenu(folderChildChild2,
								numberOfLevels, currentLevel + 1,
								orderDirection);
					} else {
						stringbuf = "<url><loc>"
								+ XMLUtils
										.xmlEscape("http://www."
												+ host.getHostname()
												+ childChild2Ident.getURI())
								+ "</loc><lastmod>"
								+ modifiedDateStringValue
								+ "</lastmod><changefreq>daily</changefreq></url>\n";

						Logger.warn(this, "Writing the XMLConfiguration Second Level Check for [" + XMLUtils
										.xmlEscape("http://www."
												+ host.getHostname()
												+ childChild2Ident.getURI()) + "]");

						writeFile(stringbuf);
						addRegistryProcessed();
					}
				} else if (childChild2 instanceof Link) {
					Link link2 = (Link) childChild2;
					if (link2.isLive() && !link2.isDeleted()) {
						if (link2.getUrl().startsWith(host.getHostname())) {
							stringbuf = "<url><loc>"
									+ XMLUtils.xmlEscape(link2.getProtocal()
											+ link2.getUrl())
									+ "</loc><lastmod>"
									+ modifiedDateStringValue
									+ "</lastmod><changefreq>daily</changefreq></url>\n";
							writeFile(stringbuf);
							addRegistryProcessed();
						}
					}
				} else if (childChild2 instanceof HTMLPage) {
					HTMLPage page2 = (HTMLPage) childChild2;
					Identifier childChild2Ident = identAPI.find(page2
							.getIdentifier());
					if (page2.isLive() && !page2.isDeleted()) {
						String indexPageConfiguration = "/index."+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

						String pathToPageUrl = XMLUtils.xmlEscape("http://www." + host.getHostname() + childChild2Ident.getURI());

						if (pathToPageUrl.endsWith(indexPageConfiguration) && isIndexPageAlreadyConfigured) {
							Logger.warn(this, "Index Page is already configured, skipping the process [" + pathToPageUrl + "]");
							continue;
						}

						pathToPageUrl = pathToPageUrl.replace(indexPageConfiguration, "");

						stringbuf = "<url><loc>" + pathToPageUrl + "</loc><lastmod>" + modifiedDateStringValue + "</lastmod><changefreq>daily</changefreq></url>\n";

						Logger.warn(this, "Writing the XMLConfiguration for an HTML Page with out index.dot extension [" + pathToPageUrl + "]");

						writeFile(stringbuf);

						addRegistryProcessed();
					}
				} else if (childChild2 instanceof com.dotmarketing.portlets.files.model.File) {
					com.dotmarketing.portlets.files.model.File file2 = (com.dotmarketing.portlets.files.model.File) childChild2;
					Identifier childChild2Ident = identAPI.find(file2
							.getIdentifier());
					if (file2.isLive() && !file2.isDeleted()) {
						stringbuf = "<url><loc>"
								+ XMLUtils.xmlEscape("http://www."
										+ host.getHostname()
										+ childChild2Ident.getURI() + "/"
										+ file2.getFileName())
								+ "</loc><lastmod>"
								+ modifiedDateStringValue
								+ "</lastmod><changefreq>daily</changefreq></url>\n";
						writeFile(stringbuf);
						addRegistryProcessed();
					}
				} else if (childChild2 instanceof Contentlet) {
					Contentlet fileContent = (Contentlet)childChild2;
					if (fileContent.isLive() && !fileContent.isArchived()) {
						Identifier identifier = APILocator.getIdentifierAPI().find(fileContent);
						stringbuf = "<url><loc>"
								+ XMLUtils.xmlEscape("http://www."
										+ host.getHostname()
										+ UtilMethods.encodeURIComponent(identifier.getParentPath()+fileContent.getStringProperty(FileAssetAPI.FILE_NAME_FIELD)))
								+ "</loc><lastmod>"
								+ modifiedDateStringValue
								+ "</lastmod><changefreq>daily</changefreq></url>\n";
						writeFile(stringbuf);
						addRegistryProcessed();
					}
				}
			}
		}

	}

	/**
	 * Create a new instance of the temporary file to save the index data
	 *
	 */
	private void openFileWriter() {
		try {
			temporaryFile = new File(Config.getStringProperty("org.dotcms.XMLSitemap.SITEMAP_XML_FILENAME","XMLSitemap")
					+ ".xml");
			out = new OutputStreamWriter(new FileOutputStream(temporaryFile),
					"UTF-8");

			out.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			out
					.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\">\n");
			out.flush();

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
	}

	/**
	 * Save in backend the new XMLSitemapGenerated.xml file and delete the
	 * temporary file
	 */
	private void closeFileWriter() {

		int counter = hostFilesCounter.get(currentHost.getHostname());
		try {
			out.write("</urlset>");
			out.flush();
			out.close();

			/******* Begin Generate compressed file *****************************/
			String dateCounter = Calendar.getInstance().get(Calendar.MONTH)
									+""+Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
									+""+Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
			String sitemapName = Config.getStringProperty("org.dotcms.XMLSitemap.SITEMAP_XML_GZ_FILENAME","XMLSitemapGenerated")
					+ dateCounter + counter + ".xml.gz";
			compressedFile = new File(sitemapName);
			GZIPOutputStream gout = new GZIPOutputStream(new FileOutputStream(
					compressedFile));

			// Open the input file
			FileInputStream in = new FileInputStream(temporaryFile);

			// Transfer bytes from the input file to the GZIP output stream
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				gout.write(buf, 0, len);
			}
			in.close();

			// Complete the GZIP file
			gout.finish();
			gout.close();
			/************ End Generate compressed file **********************/

			/* Saving file in dotCMS */

			Folder folder = folderAPI.findFolderByPath(XML_SITEMAPS_FOLDER,
					currentHost, systemUser, true);

			if (!InodeUtils.isSet(folder.getIdentifier())) {
				folder = folderAPI.createFolders(XML_SITEMAPS_FOLDER,
						currentHost, systemUser, true);
			}

			java.io.File uploadedFile = new java.io.File(sitemapName);
			// Create the new file
			Contentlet file = new Contentlet();
			file.setStructureInode(folder.getDefaultFileType());
			file.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(sitemapName));
			file.setFolder(folder.getInode());
			file.setHost(currentHost.getIdentifier());
			file.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
			if(StructureCache.getStructureByInode(file.getStructureInode()).getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET)
				file.setStringProperty("fileName", sitemapName);
			file = APILocator.getContentletAPI().checkin(file, systemUser,false);
			if(APILocator.getPermissionAPI().doesUserHavePermission(file, PermissionAPI.PERMISSION_PUBLISH, systemUser))
				APILocator.getVersionableAPI().setLive(file);
			APILocator.getVersionableAPI().setWorking(file);


		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		} finally {
			hostFilesCounter.put(currentHost.getHostname(), counter + 1);
			temporaryFile.delete();
			compressedFile.delete();
			temporaryFile = null;
			compressedFile = null;
		}
	}

	/**
	 * Write inside temporary file index pages
	 *
	 * @param data
	 */
	private void writeFile(String data) {

		try {
			if (temporaryFile == null) {
				openFileWriter();
			}

			out.write(data);
			out.flush();

			if (temporaryFile.length() > 9437184 || processedRegistries > 49999) {
				closeFileWriter();
				sitemapCounter = sitemapCounter + 1;
				processedRegistries = 0;
				openFileWriter();
			}

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}

	}

	/**
	 * Add one to the the number of pages processed counter
	 *
	 */
	private void addRegistryProcessed() {
		processedRegistries = processedRegistries + 1;
	}

	/**
	 * Delete previous XML sitemaps files from the specified host
	 *
	 * @param host
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void cleanHostFromSitemapFiles(Host host) throws Exception {

		try{
			Folder folder = folderAPI.findFolderByPath(XML_SITEMAPS_FOLDER, host,
					systemUser, false);

			if (InodeUtils.isSet(folder.getIdentifier())) {
				List<com.dotmarketing.portlets.files.model.File> files = fileAPI
						.getFolderFiles(folder, false, systemUser, true);
				for (com.dotmarketing.portlets.files.model.File file : files) {
					fileAPI.delete(file, systemUser, true);
				}
				List<Contentlet> consToDel = conAPI.findContentletsByFolder(folder, systemUser, false);
				for(Contentlet con : consToDel){
					conAPI.delete(con, systemUser, false);
				}
			}
		}
		catch(Exception e){
			Logger.error(this, e.getMessage(), e);
		}
	}
}
