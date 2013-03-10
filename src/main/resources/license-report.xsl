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
			</body>
		</html>
	</xsl:template>
	<xsl:template match="libraries">
		<table>
			<xsl:for-each select="library">
				<tr>
					<td>
						<xsl:value-of select="name" />
					</td>
					<td>
						<xsl:value-of select="license" />
					</td>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>
</xsl:stylesheet>