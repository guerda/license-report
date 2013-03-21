<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/license-report">
        <html>
            <head>
                <title>
                    License Report for
                    <xsl:value-of select="@project-name" />
                </title>
                <style type="text/css">
                    tr.even { background-color: #FAFAFA; }
                    tr.odd { background-color: #EEEEEE; }
                    tr.headline {
                    background-color: #DDDDDD; }
                    p#footer { font-family:
                    sans-serif; font-size: 8pt; text-align: right; }
                    tr.empty-license { background-color: #FFA0A0; }
                </style>
            </head>
            <body>
                <h1>
                    License Report for
                    <xsl:value-of select="@project-name" />
                </h1>
                <p>
                    Created:
                    <xsl:value-of select="@date" />
                </p>
                <xsl:apply-templates />
                <p id="footer">
                    Created with
                    <a href="http://github.com/guerda/license-report">license-report</a>
                </p>
            </body>
        </html>
    </xsl:template>
    <xsl:template match="libraries">
        <table>
            <tr class="headline">
                <th>Library</th>
                <th>License Source</th>
                <th>License Information</th>
            </tr>
            <xsl:for-each select="library">
                <xsl:variable name="row-css-class">
                    <xsl:choose>
                        <xsl:when test="position() mod 2 = 0">even</xsl:when>
                        <xsl:otherwise>odd</xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="license-row-css-class">
                    <xsl:choose>
                        <xsl:when test="information-list/information/license=''">empty-license</xsl:when>
                        <xsl:otherwise></xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <tr class="{$row-css-class} {$license-row-css-class}">
                    <xsl:variable name="count">
                        <xsl:value-of
                            select="count(descendant::information-list/information)" />
                    </xsl:variable>
                    <td rowspan="{$count}">
                        <xsl:value-of select="name" />
                    </td>
                    <xsl:for-each select="information-list/information">
                        <xsl:choose>
                            <xsl:when test="position() > 1">
                                <tr class="{$row-css-class}">
                                    <td><xsl:value-of select="source" /></td>
                                    <td><xsl:value-of select="license" /></td>
                                </tr>
                            </xsl:when>
                            <xsl:otherwise>
                                <td><xsl:value-of select="source" /></td>
                                <td><xsl:value-of select="license" /></td>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>


</xsl:stylesheet>