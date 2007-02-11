<?xml version="1.0"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:col="http://apache.org/cocoon/collection/1.0" 
  xmlns:xi="http://www.w3.org/2001/XInclude" 
  version="1.0">

  <xsl:import href="layout.xsl" />
  <xsl:output indent="yes"/>
  
  <xsl:param name="base"/>
  <xsl:param name="path"/>
  
  <xsl:param name="namespace"/>
  <xsl:param name="principal"/>

  <xsl:param name="type">content</xsl:param>
  
  <xsl:template name="middle">
    <column title="Content">
      <xsl:apply-templates select="/document/col:resource|/document/col:collection" />
    </column>
  </xsl:template>
  
  <xsl:template match="col:resource|col:collection">
    <xsl:choose>
      <xsl:when test="local-name() = 'collection'">
        <table width="100%" cellspacing="0" cellpadding="5" align="center">
          <font size="+1" face="arial,helvetica,sanserif" color="#000000">
            <tr>
              <td align="left">
                <b>Filename</b>
              </td>
              <td align="left">
                <b>Type</b>
              </td>
              <td align="left">
                <b>Size</b>
              </td>
              <td align="left">
                <b>Last Modified</b>
              </td>
              <td align="right"/>
            </tr>
            <xsl:for-each select="col:collection|col:resource">
              <tr>
                <td align="left">&#xA0;&#xA0;
                  <a href="{@name}"><xsl:value-of select="@name"/></a>
                </td>
                <td align="left">
                  <xsl:value-of select="@mime-type"/>
                </td>
                <td align="left">
                  <xsl:value-of select="@contentlength"/>
                </td>
                <td align="left">
                  <xsl:value-of select="@date"/>
                </td>
                <td align="right">
                  <form action="{$base}/delete" method="post">
                    <input type="hidden" name="parentPath" value="{$path}" />
                    <input type="hidden" name="resourceName" value="{@name}"/>
                    <input type="submit" name="doDeleteSource" value="Delete"/>
                  </form>
                </td>
              </tr>
            </xsl:for-each>
            <tr>
              <form action="{$base}/upload" method="post" enctype="multipart/form-data">
                <input type="hidden" name="parentPath" value="{$path}"/>
                <td align="left"><input type="text" name="resourceName" size="15" maxlength="40"/></td>
                <td align="left" colspan="3">
                 File:
                 <input type="file" name="uploadFile" size="15" maxlength="40"/>
                </td>
                <td align="right">
                  <input type="submit" name="doUploadSource" value="Upload File"/>
                </td>
              </form>
            </tr>
            <tr>
              <form action="{$base}/mkcol" method="post">
                <input type="hidden" name="parentPath" value="{$path}"/>
                <td align="left" colspan="4">
                  <input type="text" name="collectionName" size="15" maxlength="40"/>
                </td>
                <td align="right">
                  <input type="submit" name="doCreateCollection" value="Create collection"/>
                </td>
              </form>
            </tr>
          </font>
        </table>
      </xsl:when>
      <xsl:when test="@mime-type='image/gif'">
        <img src="../view/{$path}"/>
      </xsl:when>
      <xsl:when test="@mime-type='image/jpeg'">
        <img src="../view/{$path}"/>
      </xsl:when>
      <xsl:when test="@mime-type='text/plain'">
        <pre>
          <xi:include href="slide://{$principal}@{$namespace}/{$path}" parse="text"/>
        </pre>
      </xsl:when>
      <xsl:when test="@mime-type='text/xml'">
        <pre>
          <xi:include href="slide://{$principal}@{$namespace}/{$path}" parse="text"/>
        </pre>
      </xsl:when>
      <xsl:otherwise>
        <h3>Could not display content.</h3>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
