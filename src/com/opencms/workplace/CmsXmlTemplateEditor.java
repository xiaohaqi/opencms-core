package com.opencms.workplace;

import com.opencms.file.*;
import com.opencms.core.*;
import com.opencms.util.*;
import com.opencms.template.*;

import org.xml.sax.*;
import org.w3c.dom.*;

import java.util.*;
import java.io.*;

import javax.servlet.http.*;

/**
 * Template class for displaying the XML template editor of the OpenCms workplace.<P>
 * Reads template files of the content type <code>CmsXmlWpTemplateFile</code>.
 * 
 * @author Alexander Lucas
 * @version $Revision: 1.3 $ $Date: 2000/02/14 18:45:23 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 */
public class CmsXmlTemplateEditor extends CmsWorkplaceDefault implements I_CmsConstants {
                
    /**
     * Indicates if the results of this class are cacheable.
     * 
     * @param cms A_CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file 
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }    
    
    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters. 
     * 
     * @see getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters)
     * @param cms A_CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(A_CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(C_DEBUG && A_OpenCms.isLogging()) {
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "getting content of element " + ((elementName==null)?"<root>":elementName));
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "template file is: " + templateFile);
            A_OpenCms.log(C_OPENCMS_DEBUG, getClassName() + "selected template section is: " + ((templateSelector==null)?"<default>":templateSelector));
        }

        A_CmsRequestContext reqCont = cms.getRequestContext();
        HttpSession session = ((HttpServletRequest)reqCont.getRequest().getOriginalRequest()).getSession(true);
        CmsFile editFile = null;
        
        String content = (String)parameters.get("CONTENT");
        String file = (String)parameters.get("file");
        String exit = (String)parameters.get("EXIT");
        String save = (String)parameters.get("save");
        String body = (String)parameters.get("body");
        String bodychange = (String)parameters.get("bodychange");
        String editor = (String)parameters.get("editor");
        String title = (String)parameters.get("title");
        String layoutTemplateFilename = (String)parameters.get("template");
        String bodyElementClassName = (String)parameters.get("bodyclass");
        String bodyElementFilename = (String)parameters.get("bodyfile");
        String preview = (String)parameters.get("preview");
        String oldEdit = (String)session.getValue("te_oldedit");
        String bodytag = (String)session.getValue("bodytag");
        String oldLayoutFilename = (String)session.getValue("te_oldlayout");
        String oldTitle = (String)session.getValue("te_title");
        String oldBody = (String)session.getValue("te_oldbody");
        String layoutTemplateClassName = (String)session.getValue("te_templateclass");
        String tempPageFilename = (String)session.getValue("te_temppagefile");
        String tempBodyFilename = (String)session.getValue("te_tempbodyfile");
        
        
        boolean existsContentParam = (content!=null && (!"".equals(content)));
        boolean existsFileParam = (file!=null && (!"".equals(file)));
        boolean existsBodyParam = (body!=null && (!"".equals(body)));
        boolean existsTemplateParam = (layoutTemplateFilename!=null && (!"".equals(layoutTemplateFilename)));
        boolean saveRequested = ((save != null) && "1".equals(save));
        boolean exitRequested = ((exit != null) && "1".equals(exit));
        boolean bodychangeRequested = ((oldBody != null) && (body != null) && (!(oldBody.equals(body))));
        boolean templatechangeRequested = (oldLayoutFilename != null && layoutTemplateFilename != null
                                           && (!(oldLayoutFilename.equals(layoutTemplateFilename))));
        boolean titlechangeRequested = (oldTitle != null && title != null && (!(oldTitle.equals(title))));
        boolean previewRequested = ((preview != null) && "1".equals(preview));
        
        // Check if there is a file parameter in the request
        if(! existsFileParam) {
            throwException("No \"file\" parameter given. Don't know which file should be edited.");
        }
                
        if(!existsContentParam) {
            System.err.println("***************************");
            System.err.println("*** no content given!!! ***");
            System.err.println("***************************");

            CmsXmlControlFile originalControlFile = new CmsXmlControlFile(cms, file);
                        
            if(originalControlFile.isElementClassDefined("body")) {
                bodyElementClassName = originalControlFile.getElementClass("body");
            } 
                
            if(originalControlFile.isElementTemplateDefined("body")) {
                bodyElementFilename = originalControlFile.getElementTemplate("body");
            }
            
            if((bodyElementClassName == null) || (bodyElementFilename == null)) {
                // Either the template class or the template file 
                // for the "body" element could not be determined.
                // BUG: Send error here
            }
            
            // Check, if the selected page file is locked
            A_CmsResource pageFileResource = cms.readFileHeader(file);
            if(!pageFileResource.isLocked()) {
                // BUG: Check only, dont't lock here!
                cms.lockResource(file);
            }
                                    
            // The content file must be locked before editing            
            A_CmsResource contentFileResource = cms.readFileHeader(bodyElementFilename);
            if(!contentFileResource.isLocked()) {
                cms.lockResource(bodyElementFilename);
            }
            
            // Now get the currently selected master template file
            layoutTemplateFilename = originalControlFile.getMasterTemplate();
            layoutTemplateClassName = originalControlFile.getTemplateClass();
            
            oldLayoutFilename = layoutTemplateFilename;          
            
           if(editor == null || "".equals(editor)) {
                editor = this.C_SELECTBOX_EDITORVIEWS[C_SELECTBOX_EDITORVIEWS_DEFAULT];    
                parameters.put("editor", editor);
            }
            oldEdit = editor; 
            
            // And finally the document title
            title = cms.readMetainformation(file, C_METAINFO_TITLE);
            if(title == null) {
                title = "";
            }
            oldTitle = title;                    

            // Okay. All values are initialized. Now we can create
            // the temporary files.
            tempPageFilename = createTemporaryFile(cms, pageFileResource);
            tempBodyFilename = createTemporaryFile(cms, contentFileResource);             
            session.putValue("te_temppagefile", tempPageFilename);
            session.putValue("te_tempbodyfile", tempBodyFilename);                        
        } 
        
        if(templatechangeRequested) {
            // The user requested a change of the layout template
            System.err.println("*** TEMPLATECHANGE to " + layoutTemplateFilename);
            CmsXmlControlFile temporaryControlFile = new CmsXmlControlFile(cms, tempPageFilename);
            temporaryControlFile.setMasterTemplate(layoutTemplateFilename);
            temporaryControlFile.write();
        }
        
        if(titlechangeRequested) {
            // The user entered a new document title
            System.err.println("Changing document title to \"" + title + "\".");
            try {
                cms.writeMetainformation(tempPageFilename, C_METAINFO_TITLE, title);
            } catch(CmsException e) {
                if(A_OpenCms.isLogging()) {
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + "Could not write metainformation " + C_METAINFO_TITLE + " for file " + file + ".");                    
                    A_OpenCms.log(C_OPENCMS_INFO, getClassName() + e);
                }
            }
        }

        if(bodychangeRequested) {
            CmsXmlControlFile temporaryControlFile = new CmsXmlControlFile(cms, tempPageFilename);
            temporaryControlFile.setElementTemplSelector("body", body);
            temporaryControlFile.write();
        }
        
        // Get the XML parsed content of the layout file.
        // This can be done by calling the getOwnTemplateFile() method of the
        // layout's template class.
        // The content is needed to determine the HTML style of the body element.
        Object tempObj = CmsTemplateClassManager.getClassInstance(cms, layoutTemplateClassName);
        CmsXmlTemplate layoutTemplateClassObject = (CmsXmlTemplate)tempObj;
        CmsXmlTemplateFile layoutTemplateFile = layoutTemplateClassObject.getOwnTemplateFile(cms, layoutTemplateFilename, null, parameters, null);                   
        
        // Get the XML parsed content of the body file.        
        // This can be done by calling the getOwnTemplateFile() method of the
        // body's template class.
        tempObj = CmsTemplateClassManager.getClassInstance(cms, bodyElementClassName);
        CmsXmlTemplate bodyElementClassObject = (CmsXmlTemplate)tempObj;
        CmsXmlTemplateFile bodyTemplateFile = bodyElementClassObject.getOwnTemplateFile(cms, tempBodyFilename, "body", parameters, null);

        // Include the datablocks of the layout file into the body file.
        // So the "bodytag" and "style" data can be accessed by the body file.
        Element bodyTag = layoutTemplateFile.getBodyTag();
        bodyTemplateFile.setBodyTag(bodyTag);
        
        
        if(!existsContentParam) {
            Vector allBodys = bodyTemplateFile.getAllSections();
            if(allBodys == null || allBodys.size() == 0) {
                body = "";
            } else {
                body = (String)allBodys.elementAt(0);
            }

            CmsXmlControlFile temporaryControlFile = new CmsXmlControlFile(cms, tempPageFilename);
            temporaryControlFile.setElementTemplSelector("body", body);
            temporaryControlFile.setElementTemplate("body", tempBodyFilename);
            temporaryControlFile.write();
        }
        
        
        if(existsContentParam) {
            // save file contents to our temporary file.
            Encoder encoder = new Encoder();
            content = encoder.unescape(content);
            
            try {
                bodyTemplateFile.setEditedTemplateContent(content, oldBody, oldEdit.equals(C_SELECTBOX_EDITORVIEWS[0]));
            } catch(CmsException e) {
                if(e.getType() == e.C_XML_PARSING_ERROR) {
                    CmsXmlWpTemplateFile errorTemplate = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, templateFile, elementName, parameters, "parseerror");
                    return startProcessing(cms, errorTemplate, elementName, parameters, "parseerror");
                }
                else throw e;
            }
            bodyTemplateFile.write();
        }
        
        // If the user requested a preview then send a redirect
        // to the temporary page file.
        if(previewRequested) {
            HttpServletRequest srvReq = (HttpServletRequest)reqCont.getRequest().getOriginalRequest();
            
            String servletPath = srvReq.getServletPath();
            try {
                reqCont.getResponse().sendCmsRedirect(tempPageFilename);            
            } catch(IOException e) {
                throwException("Could not send redirect preview file " + servletPath + tempPageFilename, e);
            }
            return "".getBytes();
        }
        
        
        
        // If the user requested a "save" expilitly by pressing one of
        // the "save" buttons, copy all informations of the temporary
        // files to the original files.
        if(saveRequested) {
            commitTemporaryFile(cms, bodyElementFilename, tempBodyFilename);
            cms.writeMetainformation(file, C_METAINFO_TITLE, cms.readMetainformation(tempPageFilename, C_METAINFO_TITLE));

            CmsXmlControlFile originalControlFile = new CmsXmlControlFile(cms, file);
            CmsXmlControlFile temporaryControlFile = new CmsXmlControlFile(cms, tempPageFilename);

            originalControlFile.setMasterTemplate(temporaryControlFile.getMasterTemplate());
            originalControlFile.write();
        }
        
        // Check if we should leave th editor instead of start processing
        if(exitRequested) {
            // First delete temporary files
            cms.deleteFile(tempBodyFilename);
            cms.deleteFile(tempPageFilename);
            A_CmsXmlContent.clearFileCache(tempBodyFilename);
            A_CmsXmlContent.clearFileCache(tempPageFilename);            
            try {
                cms.getRequestContext().getResponse().sendCmsRedirect(getConfigFile(cms).getWorkplaceMainPath());
            } catch(IOException e) {
                throwException("Could not send redirect to workplace main screen.", e);
            }
            return "".getBytes();
        }

        // Load the body!                                        
        if(editor.equals(C_SELECTBOX_EDITORVIEWS[0])) {
            // HTML editor
            content = bodyTemplateFile.getEditableTemplateContent(this, parameters, body);
        } else {                                            
            content = bodyTemplateFile.getTextEditableTemplateContent(this, parameters, body);
        }
        Encoder encoder = new Encoder();
        content = encoder.escape(content);
        parameters.put("CONTENT", content);
        parameters.put("bodyfile", bodyElementFilename);
        parameters.put("bodyclass", bodyElementClassName);
        parameters.put("template", layoutTemplateFilename);
                
        // remove all parameters that could be relevant for the
        // included editor.
        parameters.remove("file");
        parameters.remove("EXIT");
        parameters.remove("save");
                        
        int numEditors = C_SELECTBOX_EDITORVIEWS.length;
        for(int i=0; i<numEditors; i++) {
            if(editor.equals(C_SELECTBOX_EDITORVIEWS[i])) {
                parameters.put("editor._CLASS_", C_SELECTBOX_EDITORVIEWS_CLASSES[i]);
                parameters.put("editor._TEMPLATE_", getConfigFile(cms).getWorkplaceTemplatePath() + C_SELECTBOX_EDITORVIEWS_TEMPLATES[i]);
            }
        }
        
        session.putValue("te_oldedit", editor);
        session.putValue("te_oldbody", body);
        session.putValue("te_oldlayout", layoutTemplateFilename);       
        session.putValue("te_title", title);       
        session.putValue("te_templateclass", layoutTemplateClassName);       
        
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        xmlTemplateDocument.setXmlData("editor", editor);
        xmlTemplateDocument.setXmlData("bodyfile", bodyElementFilename);
        xmlTemplateDocument.setXmlData("bodyclass", bodyElementClassName);
        
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters, templateSelector);
    }            

    /**
     * Gets all views available in the workplace screen.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will 
     * be filled with the appropriate information to be used for building
     * a select box.
     * <P>
     * <code>names</code> will contain language specific view descriptions
     * and <code>values</code> will contain the correspondig URL for each
     * of these views after returning from this method.
     * <P>
     * 
     * @param cms A_CmsObject Object for accessing system resources.
     * @param lang reference to the currently valid language file
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the user's current workplace view in the vectors.
     * @exception CmsException
     */
    public Integer getBodys(A_CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
            throws CmsException {
        
        String currentBodySection = (String)parameters.get("body");
        String bodyFilename = (String)parameters.get("bodyfile");
        String bodyClassName = (String)parameters.get("bodyclass");

        Object tempObj = CmsTemplateClassManager.getClassInstance(cms, bodyClassName);
        CmsXmlTemplate bodyElementClassObject = (CmsXmlTemplate)tempObj;
        CmsXmlTemplateFile bodyTemplateFile = bodyElementClassObject.getOwnTemplateFile(cms, bodyFilename, "body", parameters, null);
            
        Vector allBodys = bodyTemplateFile.getAllSections();
        int loop=0;
        int currentBodySectionIndex = 0;

        int numBodys = allBodys.size();
        for(int i=0; i<numBodys; i++) {
            String bodyname = (String)allBodys.elementAt(i);
            if(bodyname.equals(currentBodySection)) {
                currentBodySectionIndex = loop;
            }
            values.addElement(bodyname);
            names.addElement(bodyname);
            loop++;
        }
                
        return new Integer(currentBodySectionIndex);
    }    
    
    public Integer getAvailableTemplates(A_CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
            throws CmsException {
        CmsXmlWpConfigFile configFile = getConfigFile(cms);
        String templatePath = configFile.getCommonTemplatePath();
        Vector allTemplateFiles = cms.getFilesInFolder(templatePath);
        
        String currentTemplate = (String)parameters.get("template");
        int currentTemplateIndex = 0;
        
        int numTemplates = allTemplateFiles.size();
        for(int i=0; i<numTemplates; i++) {
            A_CmsResource file = (A_CmsResource)allTemplateFiles.elementAt(i);
            if(file.getState() != C_STATE_DELETED) {
                String filename = file.getName();
                String title = cms.readMetainformation(file.getAbsolutePath(), C_METAINFO_TITLE);
                if(title == null || "".equals(title)) {
                    title = file.getName();
                }            
                values.addElement(file.getAbsolutePath());
                names.addElement(title);
                if(currentTemplate.equals(file.getAbsolutePath()) 
                        || currentTemplate.equals(file.getName())) {
                    currentTemplateIndex = i;
                }
            }
        }
        return new Integer(currentTemplateIndex);
    }
        
        
        

    /** Gets all editor views available in the template editor screens.
     * <P>
     * The given vectors <code>names</code> and <code>values</code> will 
     * be filled with the appropriate information to be used for building
     * a select box.
     * <P>
     * Used to build font select boxes in editors.
     * 
     * @param cms A_CmsObject Object for accessing system resources.
     * @param lang reference to the currently valid language file
     * @param names Vector to be filled with the appropriate values in this method.
     * @param values Vector to be filled with the appropriate values in this method.
     * @param parameters Hashtable containing all user parameters <em>(not used here)</em>.
     * @return Index representing the user's current workplace view in the vectors.
     * @exception CmsException
     */
    public Integer getEditorViews(A_CmsObject cms, CmsXmlLanguageFile lang, Vector names, Vector values, Hashtable parameters) 
            throws CmsException {
        getConstantSelectEntries(names, values, C_SELECTBOX_EDITORVIEWS, lang);        
        int currentIndex = values.indexOf((String)parameters.get("editor"));        
        return new Integer(currentIndex);
    }    

    public Object setText(A_CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
            throws CmsException {
        
        Hashtable parameters = (Hashtable)userObj;
        String filename = (String)parameters.get("file");

        String content = (String)parameters.get("CONTENT");        
        boolean existsContentParam = (content!=null && (!"".equals(content)));
                
        // Check the existance of the "file" parameter
        if(! existsContentParam) {
            String errorMessage = getClassName() + "No content found.";
            if(A_OpenCms.isLogging()) {
                A_OpenCms.log(C_OPENCMS_CRITICAL, errorMessage);
            }
            return("no content");
            // throw new CmsException(errorMessage, CmsException.C_BAD_NAME);
        }
                    
        // Escape the text for including it in HTML text
        return content;
    }

     /**
     * Pre-Sets the value of the title input field.
     * This method is directly called by the content definiton.
     * @param Cms The CmsObject.
     * @param lang The language file.
     * @return Value that is pre-set into the title field.
     * @exception CmsExeption if something goes wrong.
     */
    public String setTitle(A_CmsObject cms, CmsXmlLanguageFile lang)
        throws CmsException {
        HttpSession session= ((HttpServletRequest)cms.getRequestContext().getRequest().getOriginalRequest()).getSession(true);        
        String name=(String)session.getValue("te_title");      
        return name;
    }    

    /**
     * User method to generate an URL for a preview.
     * The currently selected temporary file name will be considered.
     * <P>
     * In the editor template file, this method can be invoked by
     * <code>&lt;METHOD name="previewUrl"/&gt;</code>.
     * 
     * @param cms A_CmsObject Object for accessing system resources.
     * @param tagcontent Unused in this special case of a user method. Can be ignored.
     * @param doc Reference to the A_CmsXmlContent object of the initiating XLM document <em>(not used here)</em>.  
     * @param userObj Hashtable with parameters <em>(not used here)</em>.
     * @return String with the pics URL.
     * @exception CmsException
     */    
    public Object previewUrl(A_CmsObject cms, String tagcontent, A_CmsXmlContent doc, Object userObj) 
            throws CmsException {        
        A_CmsRequestContext reqCont = cms.getRequestContext();
        String servletPath = ((HttpServletRequest)reqCont.getRequest().getOriginalRequest()).getServletPath();
        HttpSession session = ((HttpServletRequest)reqCont.getRequest().getOriginalRequest()).getSession(false);
        String tempPath = (String)session.getValue("te_temppagefile");
        String result = servletPath + tempPath;
        System.err.println("##### " + result);
        return result;
    }
    
    private String createTemporaryFile(A_CmsObject cms, A_CmsResource file) throws CmsException{
        String temporaryFilename = file.getPath() + "_" + file.getName();
        
        // This is the code for single temporary files.
        // re-activate it, if the cms object provides a special
        // method for managing temporary files
        /* try {
            cms.copyFile(file.getAbsolutePath(), temporaryFilename);
        } catch(CmsException e) {
            if(e.getType() == e.C_FILE_EXISTS) {
                throwException("Temporary file for " + file.getName() + " already exists!", e.C_FILE_EXISTS); 
            } else {
                throwException("Could not cread temporary file for " + file.getName() + ".", e);                
            }
        }*/         
        
        A_CmsResource tempFile = null;
        String extendedTempFile = null;
        boolean ok = true;
        
        try {
            cms.copyFile(file.getAbsolutePath(), temporaryFilename);  
        } catch(CmsException e) {
            ok = false;        
        }
        
        extendedTempFile = temporaryFilename;
        
        int loop = 0;
        while(! ok) {
            ok = true;
            extendedTempFile = temporaryFilename + loop;
            try {
                cms.copyFile(file.getAbsolutePath(), extendedTempFile);  
            } catch(Exception e) {
                // temp file could not be created
                loop++;
                ok=false;
            }
        }
        
        // Oh. we have found a temporary file.
        temporaryFilename = extendedTempFile;
        return temporaryFilename;
    }        

    private void commitTemporaryFile(A_CmsObject cms, String originalFilename, String temporaryFilename)
            throws CmsException {
        CmsFile orgFile = cms.readFile(originalFilename);
        CmsFile tempFile = cms.readFile(temporaryFilename);
        
        orgFile.setContents(tempFile.getContents());
        cms.writeFile(orgFile);
        
        Hashtable minfos = cms.readAllMetainformations(temporaryFilename);
        Enumeration keys = minfos.keys();
        while(keys.hasMoreElements()) {
            String keyName = (String)keys.nextElement();
            cms.writeMetainformation(originalFilename, keyName, (String)minfos.get(keyName));
        }
    }
}
