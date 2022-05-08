package com.example.demo.jdbc.connection;

import com.example.demo.jdbc.entity.DriverEntity;
import com.example.demo.jdbc.exception.DBMetaResolverException;
import com.example.demo.jdbc.util.FileUtil;
import com.example.demo.jdbc.util.IOUtil;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.demo.jdbc.constant.Constant.*;

/**
 * @author jianjianhong
 * @date 2022/4/27
 */
@Component
public class DriverEntityRepository {
    private static final String REPOSITORY_PATH = "driver";
    private static final String REPOSITORY_FILE = "driverEntityInfo.xml";
    private File rootDirectory;
    private File driverEntityInfoFile;

    /*private volatile static DriverEntityRepository instant;

    public static DriverEntityRepository getInstance() {
        if (instant == null) {
            synchronized (DriverEntityRepository.class) {
                if (instant == null) {
                    instant = new DriverEntityRepository();
                }
            }
        }
        return instant;
    }*/

    public DriverEntityRepository() {
        String localPath = System.getProperty("user.dir");
        rootDirectory = FileUtil.getDirectory(localPath+"/"+REPOSITORY_PATH);
        driverEntityInfoFile = FileUtil.getFile(rootDirectory, REPOSITORY_FILE);
    }

    public File getDriverFile(String driverId) {
        return FileUtil.getFile(rootDirectory, driverId);
    }

    public Map<String, DriverEntity> getDriverEntityMap() throws DBMetaResolverException {
        try {
            List<DriverEntity> driverEntities = readDriverEntities();
            return driverEntities.stream().collect(Collectors.toMap(DriverEntity::getId, e->e, (k1, k2)->k1));
        }catch (Exception e) {
            throw new DBMetaResolverException(e);
        }

    }
    public List<DriverEntity> readDriverEntities() throws Exception {
        List<DriverEntity> driverEntities = null;

        if (driverEntityInfoFile.exists()) {
            InputStream inputStream = IOUtil.getInputStream(driverEntityInfoFile);
            Reader in = IOUtil.getReader(inputStream, "utf-8");

            try{
                driverEntities = readDriverEntities(in);
            } finally {
                IOUtil.close(in);
            }
        } else {
            driverEntities = new ArrayList<>();
        }

        return driverEntities;
    }

    private List<DriverEntity> readDriverEntities(Reader in) throws Exception {
        List<DriverEntity> driverEntities = new ArrayList();

        DocumentBuilderFactory documentBuilderFactory;
        DocumentBuilder documentBuilder;
        Document document;

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        document = documentBuilder.parse(new InputSource(in));

        NodeList nodeList = document.getElementsByTagName(ELEMENT_NAME_DRIVER_ENTITY);

        for (int i = 0; i < nodeList.getLength(); i++){
            DriverEntity driverEntity = new DriverEntity();

            Node node = nodeList.item(i);
            NodeList children = node.getChildNodes();

            for (int j = 0; j < children.getLength(); j++){
                Node child = children.item(j);

                String nodeName = child.getNodeName();
                String nodeContent = child.getTextContent();
                if (nodeContent != null)
                    nodeContent = nodeContent.trim();

                if (ELEMENT_NAME_ID.equalsIgnoreCase(nodeName))
                    driverEntity.setId(nodeContent);
                else if (ELEMENT_NAME_DRIVER_CLASS_NAME.equalsIgnoreCase(nodeName))
                    driverEntity.setDriverClassName(nodeContent);
                else if (ELEMENT_NAME_DISPLAY_NAME.equalsIgnoreCase(nodeName))
                    driverEntity.setDisplayName(nodeContent);
                else if (ELEMENT_NAME_DISPLAY_DESC.equalsIgnoreCase(nodeName))
                    driverEntity.setDisplayDesc(nodeContent);
                else if (ELEMENT_NAME_JRE_VERSION.equalsIgnoreCase(nodeName))
                    driverEntity.setJreVersion(nodeContent);
                else if (ELEMENT_NAME_DATABASE_NAME.equalsIgnoreCase(nodeName))
                    driverEntity.setDatabaseName(nodeContent);
                else if (ELEMENT_NAME_DATABASE_VERSIONS.equalsIgnoreCase(nodeName)){
                    NodeList dbVersionChildren = child.getChildNodes();
                    int dbVersionLength = dbVersionChildren.getLength();

                    if (dbVersionLength > 0){
                        List<String> databaseVersions = new ArrayList<String>(dbVersionLength);

                        for (int k = 0; k < dbVersionLength; k++){
                            Node dbVersionNode = dbVersionChildren.item(k);

                            if (!ELEMENT_NAME_DATABASE_VERSION.equalsIgnoreCase(dbVersionNode.getNodeName()))
                                continue;

                            String dbVersionNodeContent = dbVersionNode.getTextContent();
                            if (dbVersionNodeContent != null)
                                dbVersionNodeContent = dbVersionNodeContent.trim();

                            if (!dbVersionNodeContent.isEmpty())
                                databaseVersions.add(dbVersionNodeContent);
                        }

                        if (!databaseVersions.isEmpty())
                            driverEntity.setDatabaseVersions(databaseVersions);
                    }
                }
            }

            driverEntities.add(driverEntity);
        }

        return driverEntities;
    }

}
