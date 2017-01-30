<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:template match="ruleset">
		<html>
			<head>
				<title>
					<xsl:value-of disable-output-escaping="yes" select="@name"/>
				</title>
			</head>
			<body>
				<table>
					<tbody>
						<tr>
							<th colspan="2">
								<xsl:value-of disable-output-escaping="yes" select="description"/>
							</th>
						</tr>
						<xsl:apply-templates select="rule"/>
					</tbody>
				</table>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="rule">
		<tr>
			<td style="background-color:green;color:white;margin-left:20px;">Nombre:</td>
			<td style="background-color:green;color:white;margin-left:20px;">
				<xsl:value-of disable-output-escaping="yes" select="@name"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Categoría</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="descat"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Mensaje:</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="@message"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Descripción:</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="description"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Regla activa:</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="@active"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Beneficios</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="benefits"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Inconvenientes</td>
			<td>
				<xsl:value-of disable-output-escaping="yes" select="inconvenients"/>
			</td>
		</tr>
		<tr>
			<td valign="top">Ejemplo Incorrecto</td>
			<td>
				<textarea rows="10" cols="80">
					<xsl:value-of disable-output-escaping="yes" select="example"/>
				</textarea>
			</td>
		</tr>
		<tr>
			<td valign="top">Ejemplo correcto</td>
			<td>
				<textarea rows="10" cols="80">
					<xsl:value-of disable-output-escaping="yes" select="repair"/>
				</textarea>
			</td>
		</tr>
	</xsl:template>
</xsl:stylesheet>
