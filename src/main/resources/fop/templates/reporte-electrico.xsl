<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="/reporte">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="pagina"
            page-height="29.7cm" page-width="21cm"
            margin-top="1.2cm" margin-bottom="1.2cm"
            margin-left="1.5cm" margin-right="1.5cm">
          <fo:region-body margin-top="1.4cm" margin-bottom="0.8cm"/>
          <fo:region-before extent="1cm"/>
          <fo:region-after extent="0.8cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="pagina">

        <fo:static-content flow-name="xsl-region-before">
          <fo:block font-size="9pt" color="#666666" text-align="right">
            AW-SHEF · CENIDET — Reporte de Variables Eléctricas
          </fo:block>
        </fo:static-content>

        <fo:static-content flow-name="xsl-region-after">
          <fo:block font-size="8pt" color="#999999" text-align="center">
            Página <fo:page-number/> de <fo:page-number-citation ref-id="ultima-pagina"/>
          </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

          <fo:block font-size="16pt" font-weight="bold" color="#003B8E" space-after="4pt">
            Reporte de Variables Eléctricas
          </fo:block>
          <fo:block font-size="10pt" color="#555555" space-after="14pt">
            Periodo: <xsl:value-of select="periodo/@inicio"/> — <xsl:value-of select="periodo/@fin"/>
          </fo:block>

          <fo:table table-layout="fixed" width="100%" border="0.5pt solid #cccccc">
            <fo:table-column column-width="20%"/>
            <fo:table-column column-width="20%"/>
            <fo:table-column column-width="20%"/>
            <fo:table-column column-width="20%"/>
            <fo:table-column column-width="20%"/>

            <fo:table-header>
              <fo:table-row background-color="#003B8E">
                <fo:table-cell padding="4pt"><fo:block color="white" font-size="9pt" font-weight="bold">Fecha/Hora</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-size="9pt" font-weight="bold">Corriente (A)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-size="9pt" font-weight="bold">Voltaje (V)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-size="9pt" font-weight="bold">Potencia (W)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-size="9pt" font-weight="bold">Energía (kWh)</fo:block></fo:table-cell>
              </fo:table-row>
            </fo:table-header>

            <fo:table-body>
              <xsl:for-each select="lecturas/lectura">
                <fo:table-row>
                  <xsl:if test="position() mod 2 = 0">
                    <xsl:attribute name="background-color">#f4f6f8</xsl:attribute>
                  </xsl:if>
                  <fo:table-cell padding="3pt"><fo:block font-size="8pt"><xsl:value-of select="@fecha"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block font-size="8pt"><xsl:value-of select="corriente"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block font-size="8pt"><xsl:value-of select="voltaje"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block font-size="8pt"><xsl:value-of select="potencia"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block font-size="8pt"><xsl:value-of select="energia"/></fo:block></fo:table-cell>
                </fo:table-row>
              </xsl:for-each>
            </fo:table-body>
          </fo:table>

          <fo:block id="ultima-pagina"/>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
