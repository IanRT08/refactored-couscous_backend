<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <xsl:output method="xml" indent="yes"/>

  <xsl:param name="logoBase64"/>

  <xsl:template match="/reporte">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="pagina"
            page-height="29.7cm" page-width="21cm"
            margin-top="1.2cm" margin-bottom="1.2cm"
            margin-left="1.5cm" margin-right="1.5cm">
          <fo:region-body margin-top="1.2cm" margin-bottom="0.8cm"/>
          <fo:region-before extent="1cm"/>
          <fo:region-after extent="0.8cm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>

      <fo:page-sequence master-reference="pagina">

        <!-- ── Encabezado de página ─────────────────────────────────── -->
        <fo:static-content flow-name="xsl-region-before">
          <fo:block font-size="8pt" color="#888888" text-align="right" border-bottom="0.3pt solid #cccccc" padding-bottom="3pt">
            AW-SHEF · CENIDET — Reporte de Variables Eléctricas
          </fo:block>
        </fo:static-content>

        <!-- ── Pie de página ─────────────────────────────────────────── -->
        <fo:static-content flow-name="xsl-region-after">
          <fo:block font-size="8pt" color="#999999" text-align="center">
            Página <fo:page-number/> de <fo:page-number-citation ref-id="ultima-pagina"/>
          </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

          <!-- ── Bloque institucional (dos columnas) ─────────────────── -->
          <fo:table table-layout="fixed" width="100%" space-after="12pt">
            <fo:table-column column-width="62%"/>
            <fo:table-column column-width="38%"/>
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell display-align="center">
                  <fo:block font-size="15pt" font-weight="bold" color="#003B8E">
                    Reporte de Variables Eléctricas
                  </fo:block>
                  <fo:block font-size="9pt" color="#555555" space-before="3pt">
                    Estación AW-SHEF · Fuente: <xsl:value-of select="periodo/@fuente"/>
                  </fo:block>
                  <fo:block font-size="9pt" color="#555555" space-before="1pt">
                    Periodo: <xsl:value-of select="periodo/@inicio"/> — <xsl:value-of select="periodo/@fin"/>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell text-align="right" display-align="center">
                  <xsl:choose>
                    <xsl:when test="$logoBase64 != ''">
                      <fo:block text-align="right">
                        <fo:external-graphic content-height="2.4cm" scaling="uniform">
                          <xsl:attribute name="src">
                            <xsl:text>url('data:image/png;base64,</xsl:text>
                            <xsl:value-of select="$logoBase64"/>
                            <xsl:text>')</xsl:text>
                          </xsl:attribute>
                        </fo:external-graphic>
                      </fo:block>
                    </xsl:when>
                    <xsl:otherwise>
                      <fo:block font-size="14pt" font-weight="bold" color="#003B8E" font-style="italic">CENIDET</fo:block>
                      <fo:block font-size="7pt" color="#555555" space-before="2pt">Centro Nacional de Investigación</fo:block>
                      <fo:block font-size="7pt" color="#555555">y Desarrollo Tecnológico</fo:block>
                      <fo:block font-size="7pt" color="#777777" space-before="1pt">Cuernavaca, Morelos</fo:block>
                    </xsl:otherwise>
                  </xsl:choose>
                </fo:table-cell>
              </fo:table-row>
            </fo:table-body>
          </fo:table>

          <!-- ── Tabla de datos ──────────────────────────────────────── -->
          <fo:table table-layout="fixed" width="100%" border="0.5pt solid #cccccc"
                    font-size="8pt">
            <fo:table-column column-width="17%"/>
            <fo:table-column column-width="17%"/>
            <fo:table-column column-width="17%"/>
            <fo:table-column column-width="17%"/>
            <fo:table-column column-width="16%"/>
            <fo:table-column column-width="16%"/>

            <fo:table-header>
              <fo:table-row background-color="#003B8E">
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Fecha/Hora</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Voltaje (V)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Corriente (A)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Potencia (W)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Voc (V)</fo:block></fo:table-cell>
                <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Energía (Wh)</fo:block></fo:table-cell>
              </fo:table-row>
            </fo:table-header>

            <fo:table-body>
              <xsl:for-each select="lecturas/lectura">
                <fo:table-row>
                  <xsl:if test="position() mod 2 = 0">
                    <xsl:attribute name="background-color">#e8f0fb</xsl:attribute>
                  </xsl:if>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="@fecha"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="voltaje"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="corriente"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="potencia"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="voc"/></fo:block></fo:table-cell>
                  <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="energia"/></fo:block></fo:table-cell>
                </fo:table-row>
              </xsl:for-each>
            </fo:table-body>
          </fo:table>

          <!-- ── Estadísticas del periodo ──────────────────────────── -->
          <xsl:if test="count(resumen/fila) &gt; 0">
            <fo:block space-before="12pt" font-size="10pt" font-weight="bold" color="#003B8E" space-after="5pt"
                      keep-with-next.within-page="always">
              Estadísticas del Periodo
            </fo:block>
            <fo:table table-layout="fixed" width="100%" border="0.5pt solid #cccccc"
                      font-size="8pt" keep-together.within-page="always">
              <fo:table-column column-width="17%"/>
              <fo:table-column column-width="17%"/>
              <fo:table-column column-width="17%"/>
              <fo:table-column column-width="17%"/>
              <fo:table-column column-width="16%"/>
              <fo:table-column column-width="16%"/>
              <fo:table-header>
                <fo:table-row background-color="#1B5E20">
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Estadística</fo:block></fo:table-cell>
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Voltaje (V)</fo:block></fo:table-cell>
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Corriente (A)</fo:block></fo:table-cell>
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Potencia (W)</fo:block></fo:table-cell>
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Voc (V)</fo:block></fo:table-cell>
                  <fo:table-cell padding="4pt"><fo:block color="white" font-weight="bold">Energía (Wh)</fo:block></fo:table-cell>
                </fo:table-row>
              </fo:table-header>
              <fo:table-body>
                <xsl:for-each select="resumen/fila">
                  <fo:table-row>
                    <xsl:if test="position() mod 2 = 0">
                      <xsl:attribute name="background-color">#e8f5e9</xsl:attribute>
                    </xsl:if>
                    <fo:table-cell padding="3pt"><fo:block font-weight="bold"><xsl:value-of select="@etiqueta"/></fo:block></fo:table-cell>
                    <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="voltaje"/></fo:block></fo:table-cell>
                    <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="corriente"/></fo:block></fo:table-cell>
                    <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="potencia"/></fo:block></fo:table-cell>
                    <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="voc"/></fo:block></fo:table-cell>
                    <fo:table-cell padding="3pt"><fo:block><xsl:value-of select="energia"/></fo:block></fo:table-cell>
                  </fo:table-row>
                </xsl:for-each>
              </fo:table-body>
            </fo:table>
          </xsl:if>

          <!-- ── Gráfica del periodo ────────────────────────────────── -->
          <xsl:if test="string-length(grafica) &gt; 0">
            <fo:block space-before="14pt" font-size="10pt" font-weight="bold" color="#003B8E" space-after="6pt"
                      keep-with-next.within-page="always">
              Gráfica del Periodo
            </fo:block>
            <fo:block keep-together.within-page="always">
              <fo:external-graphic content-width="17cm" content-height="20cm" scaling="uniform">
                <xsl:attribute name="src">
                  <xsl:text>url('data:image/png;base64,</xsl:text>
                  <xsl:value-of select="grafica"/>
                  <xsl:text>')</xsl:text>
                </xsl:attribute>
              </fo:external-graphic>
            </fo:block>
          </xsl:if>

          <!-- ── Citación / agradecimiento ─────────────────────────── -->
          <fo:block space-before="16pt" border-top="0.5pt solid #cccccc" padding-top="8pt"
                    font-size="8pt" color="#666666" font-style="italic">
            <fo:inline font-weight="bold" font-style="normal">Nota: </fo:inline>
            Si utilizas estos datos en algún trabajo académico o de investigación, incluye la siguiente referencia:
          </fo:block>
          <fo:block font-size="8pt" color="#555555" font-style="italic" space-before="3pt" margin-left="0.8cm">
            Estación AW-SHEF, Centro Nacional de Investigación y Desarrollo Tecnológico (CENIDET), Cuernavaca, Morelos. Datos obtenidos del sistema de monitoreo AW-SHEF.
          </fo:block>

          <fo:block id="ultima-pagina"/>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
