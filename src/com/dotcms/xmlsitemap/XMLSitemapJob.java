package com.dotcms.xmlsitemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.business.FileFactory;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
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

	public XMLSitemapJob () {
		try {
			systemUser = userAPI.getSystemUser();

			hostFilesCounter = new HashMap<String, Integer>();

			XML_SITEMAPS_FOLDER = Config.getStringProperty( "org.dotcms.XMLSitemap.XML_SITEMAPS_FOLDER", "/XMLSitemaps/" );
			String usePermalinksString = Config.getStringProperty( "org.dotcms.XMLSitemap.USE_PERMALINKS=false", "false" );
			String useStructureURLMapString = Config.getStringProperty( "org.dotcms.XMLSitemap.USE_STRUCTURE_URL_MAP", "false" );
			usePermalinks = (UtilMethods.isSet( usePermalinksString ) ? Boolean.parseBoolean( usePermalinksString ) : false);
			useStructureURLMap = (UtilMethods.isSet( useStructureURLMapString ) ? Boolean.parseBoolean( useStructureURLMapString ) : false);

			modifiedDateStringValue = UtilMethods.dateToHTMLDate(
					new java.util.Date( System.currentTimeMillis() ), "yyyy-MM-dd" );

			structuresToIgnoreConfig = Config.getStringProperty( "org.dotcms.XMLSitemap.IGNORE_Structure_Ids", "" );

			//generateSitemapPerHost();
		} catch ( Exception e ) {
			Logger.error( this, e.getMessage(), e );
		}
	}

	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			systemUser = userAPI.getSystemUser();
			generateSitemapPerHost();

		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
		}
		finally {
		    try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, e.getMessage(), e);
            }
		    finally {
		        DbConnectionFactory.closeConnection();
		    }
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
	public void generateSitemapPerHost() throws DotDataException, DotSecurityException {

		List<Host> hostsList = hostAPI.findAll(systemUser, false);
		List<String> structureVelocityVarNames = StructureFactory.getAllVelocityVariablesNames();

		for (Host host : hostsList) {

            if ( host.isSystemHost() ) {
				continue;
            }

			processedRegistries = 0;
			currentHost = host;
			sitemapCounter = 1;
            String stringbuf;

			try {
				/**
				 * mark all the existing sitemaps generated in the XMLSitemap
				 * folder for the host specified to be removed upon creating new ones
				 */
				List<Object> oldSiteMapsToDel = new ArrayList<Object>();

				Folder folder = folderAPI.findFolderByPath(XML_SITEMAPS_FOLDER, host, systemUser, false);

				if (InodeUtils.isSet(folder.getIdentifier())) {
					oldSiteMapsToDel.addAll(fileAPI.getFolderFiles(folder, false, systemUser, true));
					oldSiteMapsToDel.addAll(conAPI.findContentletsByFolder(folder, systemUser, false));
				}

				hostFilesCounter.put(host.getHostname(), sitemapCounter);

				/* adding host url */
				stringbuf = "<url><loc>"
						+ XMLUtils.xmlEscape("http://" + host.getHostname() + "/")
						+ "</loc><lastmod>"
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

					if (ignorableStructureIds.contains(stVelocityVarName.toLowerCase())) {
						continue;
					}

					Structure st = StructureFactory.getStructureByVelocityVarName( stVelocityVarName );

                    //Continue only if have a detail
                    if ( !InodeUtils.isSet( st.getPagedetail() ) ) {
						continue;
                    }

					//Getting the detail page, that detail page could be a HTMLPageAsset or a legacy page
					IHTMLPage page = null;
					try {
						//First lets asume it is a HTMLPageAsset
						Contentlet contentlet = APILocator.getContentletAPI().search( "+identifier:" + st.getPagedetail() + " +live:true", 0, 0, "moddate", systemUser, false ).get( 0 );
						if ( contentlet != null ) {
							page = APILocator.getHTMLPageAssetAPI().fromContentlet( contentlet );
						}
					} catch ( DotContentletStateException e ) {
						//HTMLPageAsset not found, now lets try to find a legacy page
						page = htmlAPI.loadLivePageById( st.getPagedetail(), systemUser, true );
					}

					if ( !UtilMethods.isSet( page ) || !UtilMethods.isSet( page.getIdentifier() ) ) {
						Logger.error( this, "Unable to find detail page for structure [" + stVelocityVarName + "]." );
						continue;
					}

					Identifier pageIdentifier = identAPI.find( page.getIdentifier() );
					if ( !UtilMethods.isSet( pageIdentifier ) || !UtilMethods.isSet( pageIdentifier.getId() ) ) {
						Logger.error( this, "Unable to find detail page for structure [" + stVelocityVarName + "]." );
						continue;
					}

					Logger.debug( this, " Creating Site Map for Structure " + stVelocityVarName );

					//Search for the content of this structure
					String hostQuery = "+(conhost:" + host.getIdentifier() + " conhost:SYSTEM_HOST)";
					String query = hostQuery + " +structureName:" + st.getVelocityVarName() + " +deleted:false +live:true";

					List<Contentlet> hits = conAPI.search( query, -1, 0, "", systemUser, true );
					String structureURLMap = st.getUrlMapPattern();

					List<RegExMatch> matches = null;

					if ( useStructureURLMap && UtilMethods.isSet( structureURLMap ) ) {
						matches = RegEX.find( st.getUrlMapPattern(), "({[^{}]+})" );
					}

					for (Contentlet contenlet : hits) {
						try {
							if (usePermalinks) {
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://"
												+ host.getHostname()
												+ "/permalink/"
												+ contenlet.getIdentifier()
												+ "/" + st.getPagedetail()
												+ "/")
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";

							} else if (useStructureURLMap && UtilMethods.isSet(structureURLMap) && (matches != null)) {

								String uri = structureURLMap;
								Logger.debug(this, " Found the URL String for validation [" + uri + "]");

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

								if ( uri == null && UtilMethods.isSet( st.getDetailPage() ) ) {
									if ( page != null && UtilMethods.isSet( page.getIdentifier() ) ) {
										uri = page.getURI() + "?id=" + contenlet.getInode();
									}
								}
								String urlRelacementText = getUrlPatternReplacementText( host, stVelocityVarName);

								uri = uri.replaceAll(urlRelacementText, "");

								Logger.debug(this,
										"Performing URL replacement - urlRelacementText ["
												+ urlRelacementText
												+ "], uri [" + uri + "]");

								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://"
												+ host.getHostname() + uri)
										+ "</loc><lastmod>"
										+ modifiedDateStringValue
										+ "</lastmod><changefreq>daily</changefreq></url>\n";
							} else {
								stringbuf = "<url><loc>"
										+ XMLUtils.xmlEscape("http://"
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

                /*
                 This part add the show on menu pages. similar as we do in nav tool to generate the sitemap
                 */
                List<Folder> itemsList = folderAPI.findSubFolders( host, true );
                if ( itemsList != null && !itemsList.isEmpty() ) {

                    // /FIRST LEVEL MENU ITEMS!!!!
                    for ( Object itemChild : itemsList ) {

						if (itemChild instanceof Folder) {

							Folder folderChild = (Folder) itemChild;

							Logger.debug(this, "Folder Iteration in progress Name [" + folderChild.getName() + "], show on Menu Indicator [" + folderChild.isShowOnMenu() + "]");

							// recursive method here
							buildSubFolderSiteMapMenu(folderChild, 100, 1, 1);

						} else if (itemChild instanceof Link) {

                            writeLink( host, (Link) itemChild );

						} else if (itemChild instanceof IHTMLPage) {

                            writeHTMLPage( host, (IHTMLPage) itemChild, false );

						} else if (itemChild instanceof com.dotmarketing.portlets.files.model.File) {

                            writeFile( host, (com.dotmarketing.portlets.files.model.File) itemChild );

						} else if (itemChild instanceof Contentlet) {

							writeContentlet( host, (Contentlet) itemChild );

						}
					}

				}
				
				cleanOldSitemapFiles(oldSiteMapsToDel);

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
	 * @param thisFolder
	 * @param numberOfLevels
	 * @param currentLevel
	 * @param orderDirection
     * @throws com.dotmarketing.exception.DotDataException
     * @throws com.dotmarketing.exception.DotSecurityException
	 */
	@SuppressWarnings("unchecked")
	private void buildSubFolderSiteMapMenu ( Folder thisFolder, int numberOfLevels, int currentLevel, int orderDirection ) throws DotDataException, DotSecurityException {

		String stringbuf;
		// gets menu items for this folder
		List<Inode> itemsChildrenList2 = folderAPI.findMenuItems( thisFolder, orderDirection );

		Identifier folderIdent = identAPI.find( thisFolder.getIdentifier() );
		Host host = hostAPI.findParentHost( thisFolder, systemUser, false );
		//The folder have a index page?, if don't we don' need to add it to the site map
		Identifier indexPageId = identAPI.loadFromCache( host, folderIdent.getURI() + "/" + CMSFilter.CMS_INDEX_PAGE );

		Logger.debug( this, "Performing check for folders [" + (folderIdent.getURI() + "/" + CMSFilter.CMS_INDEX_PAGE) + "], Identifier Check ["
				+ (indexPageId != null) + "], Children Count [" + itemsChildrenList2.size() + "], Host Identifier [" + host.getIdentifier() + "], Identifier " +
				((indexPageId != null) ? indexPageId.getInode() : "") + "]" );

		boolean isIndexPageAlreadyConfigured = false;

		if ( (indexPageId != null) && InodeUtils.isSet( indexPageId.getInode() ) ) {

			stringbuf = "<url><loc>"
					+ XMLUtils
					.xmlEscape( "http://"
							+ host.getHostname()
							+ folderIdent.getURI() )
					+ "</loc><lastmod>" + modifiedDateStringValue
					+ "</lastmod><changefreq>daily</changefreq></url>\n";

			Logger.debug( this, "Writing the XMLConfiguration for Folder[" + XMLUtils.xmlEscape( "http://" + host.getHostname() + folderIdent.getURI() ) + "]" );

			isIndexPageAlreadyConfigured = true;

			writeFile( stringbuf );
			addRegistryProcessed();
		}

		if ( currentLevel < numberOfLevels ) {

			for ( Permissionable childChild2 : itemsChildrenList2 ) {
				if ( childChild2 instanceof Folder ) {
					Folder folderChildChild2 = (Folder) childChild2;
					Identifier childChild2Ident = identAPI
							.find( folderChildChild2.getIdentifier() );

					if ( currentLevel <= numberOfLevels ) {
						buildSubFolderSiteMapMenu( folderChildChild2,
								numberOfLevels, currentLevel + 1,
								orderDirection );
					} else {
						stringbuf = "<url><loc>"
								+ XMLUtils
								.xmlEscape( "http://"
										+ host.getHostname()
										+ childChild2Ident.getURI() )
								+ "</loc><lastmod>"
								+ modifiedDateStringValue
								+ "</lastmod><changefreq>daily</changefreq></url>\n";

						Logger.debug( this, "Writing the XMLConfiguration Second Level Check for [" + XMLUtils
								.xmlEscape( "http://"
										+ host.getHostname()
										+ childChild2Ident.getURI() ) + "]" );

						writeFile( stringbuf );
						addRegistryProcessed();
					}
				} else if ( childChild2 instanceof Link ) {

					writeLink( host, (Link) childChild2 );

				} else if ( childChild2 instanceof IHTMLPage ) {

					writeHTMLPage( host, (IHTMLPage) childChild2, isIndexPageAlreadyConfigured );

				} else if ( childChild2 instanceof com.dotmarketing.portlets.files.model.File ) {

					writeFile( host, (com.dotmarketing.portlets.files.model.File) childChild2 );

				} else if ( childChild2 instanceof Contentlet ) {

					writeContentlet( host, (Contentlet) childChild2 );

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

			out.write( "<?xml version='1.0' encoding='UTF-8'?>\n" );
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
									+""+Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
									+""+Calendar.getInstance().get(Calendar.MINUTE);
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
            if ( CacheLocator.getContentTypeCache().getStructureByInode( file.getStructureInode() ).getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET ) {
				file.setStringProperty("fileName", sitemapName);
            }
			file = APILocator.getContentletAPI().checkin(file, systemUser,false);
            if ( APILocator.getPermissionAPI().doesUserHavePermission( file, PermissionAPI.PERMISSION_PUBLISH, systemUser ) ) {
				APILocator.getVersionableAPI().setLive(file);
            }
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
	 */
	private void addRegistryProcessed() {
		processedRegistries = processedRegistries + 1;
	}

	/**
	 * Delete previous XML sitemaps files from the specified host
	 *
     * @param siteMapsToDel
	 * @throws Exception
	 */
	private void cleanOldSitemapFiles(List<Object> siteMapsToDel) throws Exception {

		try{
			for(Object siteMap : siteMapsToDel){
				if(siteMap instanceof com.dotmarketing.portlets.files.model.File){
					fileAPI.delete((com.dotmarketing.portlets.files.model.File)siteMap, systemUser, true);
				}else if(siteMap instanceof Contentlet){
					conAPI.delete((Contentlet)siteMap, systemUser, false);
				}
			}
		}
		catch(Exception e){
			Logger.error(this, e.getMessage(), e);
		}
	}

	private void writeContentlet ( Host host, Contentlet contentlet ) throws DotDataException, DotSecurityException {

		if ( contentlet.isLive() && !contentlet.isArchived() ) {

			Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );
			String url = identifier.getParentPath() + contentlet.getStringProperty( FileAssetAPI.FILE_NAME_FIELD );

			String stringbuf = "<url><loc>"
					+ XMLUtils.xmlEscape( "http://"
					+ host.getHostname()
					+ UtilMethods.encodeURIComponent( url ) )
					+ "</loc><lastmod>"
					+ modifiedDateStringValue
					+ "</lastmod><changefreq>daily</changefreq></url>\n";

			writeFile( stringbuf );
			addRegistryProcessed();
		}
	}

	private void writeFile ( Host host, com.dotmarketing.portlets.files.model.File file ) throws DotDataException, DotSecurityException {

		Identifier childChild2Ident = identAPI.find( file.getIdentifier() );
		if ( file.isLive() && !file.isDeleted() ) {

			String stringbuf = "<url><loc>"
					+ XMLUtils.xmlEscape( "http://"
					+ host.getHostname()
					+ childChild2Ident.getURI() + "/"
					+ file.getFileName() )
					+ "</loc><lastmod>"
					+ modifiedDateStringValue
					+ "</lastmod><changefreq>daily</changefreq></url>\n";

			writeFile( stringbuf );
			addRegistryProcessed();
		}
	}

	private void writeHTMLPage ( Host host, IHTMLPage page, Boolean isIndexPageAlreadyConfigured ) throws DotDataException, DotSecurityException {

		Identifier childChild2Ident = identAPI.find( page.getIdentifier() );
		if ( page.isLive() && !page.isArchived() ) {

			String indexPageConfiguration = "/" + CMSFilter.CMS_INDEX_PAGE;
			String pathToPageUrl = XMLUtils.xmlEscape( "http://" + host.getHostname() + childChild2Ident.getURI() );

			if ( pathToPageUrl.endsWith( indexPageConfiguration ) && isIndexPageAlreadyConfigured ) {
				Logger.debug( this, "Index Page is already configured, skipping the process [" + pathToPageUrl + "]" );
				return;
			}

			pathToPageUrl = pathToPageUrl.replace( indexPageConfiguration, "" );

			String stringbuf = "<url><loc>"
					+ pathToPageUrl
					+ "</loc><lastmod>"
					+ modifiedDateStringValue
					+ "</lastmod><changefreq>daily</changefreq></url>\n";

			writeFile( stringbuf );
			addRegistryProcessed();
		}
	}

	private void writeLink ( Host host, Link link ) throws DotSecurityException, DotDataException {

		if ( link.isLive() && !link.isDeleted() ) {
			if ( link.getUrl().startsWith( host.getHostname() ) ) {

				String stringbuf = "<url><loc>"
						+ XMLUtils.xmlEscape( link.getProtocal()
						+ link.getUrl() )
						+ "</loc><lastmod>"
						+ modifiedDateStringValue
						+ "</lastmod><changefreq>daily</changefreq></url>\n";

				writeFile( stringbuf );
				addRegistryProcessed();
			}
		}
	}

}