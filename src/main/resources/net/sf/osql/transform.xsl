<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:x="alias://xsl">

	<xsl:output method="xml" />

	<xsl:namespace-alias stylesheet-prefix="x" result-prefix="xsl"/>

	<xsl:variable name="mode"><mode>soft</mode></xsl:variable>

	<xsl:variable name="vars" select="/table/define/var"/>

	<xsl:template match="/">
		<root>
			<x:stylesheet version="2.0">
				<x:output method="text" indent="yes"/>
				<x:template match="/table">
					<xsl:call-template name="definition"/>
				</x:template>
			</x:stylesheet>
			<x:stylesheet version="2.0">
				<x:output method="text" indent="yes"/>
				<x:template match="/table">
					<xsl:call-template name="constraints"/>
				</x:template>
			</x:stylesheet>
			<x:stylesheet version="2.0">
				<x:output method="text" indent="yes"/>
				<x:template match="/table">
					<xsl:call-template name="insertions"/>
				</x:template>
			</x:stylesheet>
			<x:stylesheet version="2.0">
				<x:output method="text" indent="yes"/>
				<x:template match="/table">
					<xsl:call-template name="triggers"/>
				</x:template>
			</x:stylesheet>
			<x:stylesheet version="2.0">
				<x:output method="text" indent="yes"/>
				<x:template match="/table">
					<xsl:call-template name="itable"/>
				</x:template>
			</x:stylesheet>
		</root>
	</xsl:template>

	<xsl:template name="definition">
		<x:value-of select="sql"/>
		<xsl:apply-templates select="table/definition" mode="normal"/>
	</xsl:template>

	<xsl:template name="constraints">
		<x:for-each select="constraints/constraint">
			<xsl:apply-templates select="table/constraints" mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template name="insertions">
		<x:for-each select="insertions/insertion">
			<xsl:apply-templates select="table/insertion" mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template name="triggers">
		<xsl:apply-templates select="table/triggers" mode="normal"/>
	</xsl:template>

	<xsl:template name="itable">
		<xsl:apply-templates select="table/itable" mode="normal"/>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='insert'][@event='before']" mode="normal" priority="3">
		<xsl:apply-templates mode="normal"/>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='insert'][@event='after']" mode="normal" priority="3">
		<x:if test="subtypes/subtype | triggers/trigger[@event='after:insert'] | /table[@dependent='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='update'][@event='before']" mode="normal" priority="3">
		<x:if test="/table[@from] | triggers/trigger[@event='before:update'] | columns/column[@composition='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='update'][@event='after']" mode="normal" priority="3">
		<x:if test="triggers/trigger[@event='after:update'] | columns/column[@inherit='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='delete'][@event='before']" mode="normal" priority="3">
		<x:if test="triggers/trigger[@event='before:delete']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="table/triggers/trigger[@action='delete'][@event='after']" mode="normal" priority="3">
		<x:if test="/table[@from] | triggers/trigger[@event='after:delete'] | columns/column[@composition='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link" mode="normal"/>
	<xsl:template match="var" mode="normal"/>
	<xsl:template match="var" mode="resolv"><xsl:apply-templates mode="normal"/></xsl:template>

	<xsl:template match="call" mode="normal">
		<x:variable name="param">
			<xsl:apply-templates mode="normal"/>
		</x:variable>
		<x:for-each select="$param">
			<xsl:copy-of select="$vars[@name=./@name]"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="join" mode="normal">
		<x:variable name="list">
			<xsl:apply-templates mode="normal"/>
		</x:variable>
		<x:value-of select="&#36;list/item[position()=1]"/>
		<x:for-each select="&#36;list/item[position()!=1]">
			<xsl:value-of select="@delimiter"/><x:value-of select="."/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="join//item" mode="normal"  priority="2">
		<xsl:copy>
			<xsl:apply-templates mode="normal"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="use[@name='name']" mode="normal" priority="1">
		<x:value-of select="@name"/>
	</xsl:template>

	<xsl:template match="use[@name='from']" mode="normal" priority="1">
		<x:value-of select="@from"/>
	</xsl:template>

	<xsl:template match="use[@name='type']" mode="normal" priority="1">
		<x:value-of select="@type"/>
	</xsl:template>

	<xsl:template match="link[@name='root']" mode="normal" priority="1">
		<x:if test="not(@from)">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='child']" mode="normal" priority="1">
		<x:if test="@from">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='dependent']" mode="normal" priority="1">
		<x:if test="@dependent='true'">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='parent']" mode="normal" priority="1">
		<x:if test="subtypes/subtype">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='siblings']" mode="normal" priority="1">
		<x:for-each select="siblings/sibling">
			<item><x:value-of select="@name"/></item>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='subtype']" mode="normal" priority="1">
		<x:for-each select="subtypes/subtype">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='link']" mode="normal" priority="1">
		<x:for-each select="links/column">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='inherited']" mode="normal" priority="1">
		<x:for-each select="columns/column[@present='true'][not(@new='true')]">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="use[@name='subtypes']" mode="normal" priority="1">
		<x:for-each select="subtypes/subtype">
			<item><x:value-of select="@name"/></item>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='abstract']" mode="normal" priority="1">
		<x:if test="@abstract='true'">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='concrete']" mode="normal" priority="1">
		<x:if test="not(@abstract='true')">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='parent']//link[@name='notnull']" mode="normal" priority="2">
		<x:if test="@abstract='true'">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']" mode="normal" priority="1">
		<x:for-each select="columns/column">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='columns']//link[@name='present']" mode="normal" priority="2">
		<x:if test="@present='true'">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']//link[@name='notnull']" mode="normal" priority="2">
		<x:if test="not(@null='true')">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']//link[@name='unique']" mode="normal" priority="2">
		<x:if test="@unique='true'">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']//link[@name='default']" mode="normal" priority="2">
		<x:if test="@default">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']//use[@name='default']" mode="normal" priority="2">
		<x:value-of select="@default"/>
	</xsl:template>

	<xsl:template match="link[@name='columns']//link[@name='comment']" mode="normal" priority="2">
		<x:if test="@comment">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='columns']//use[@name='comment']" mode="normal" priority="2">
		<x:value-of select="@comment"/>
	</xsl:template>

	<xsl:template match="link[@name='index']" mode="normal" priority="1">
		<x:for-each select="index/list">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='index' or @name='unique']//use[@name='name']" mode="normal" priority="2">
		<x:value-of select="@name"/>
	</xsl:template>

	<xsl:template match="link[@name='index' or @name='unique']//use[@name='columns']" mode="normal" priority="2">
		<x:for-each select="column">
			<item><x:value-of select="@name"/></item>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='unique']" mode="normal" priority="1">
		<x:for-each select="unique/list">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="constraints//use[@name='name']" mode="normal" priority="2">
		<x:value-of select="/table/@name"/>
	</xsl:template>

	<xsl:template match="constraints//use[@name='number']" mode="normal" priority="2">
		<x:value-of select="@number"/>
	</xsl:template>

	<xsl:template match="constraints//use[@name='reference']" mode="normal" priority="2">
		<x:value-of select="@reference"/>
	</xsl:template>

	<xsl:template match="constraints//use[@name='target']" mode="normal" priority="2">
		<x:value-of select="@target"/>
	</xsl:template>

	<xsl:template match="constraints//link[@name='setnull']" mode="normal" priority="2">
		<x:if test="@composition">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='link']//use[@name='sname']" mode="normal" priority="2">
		<x:value-of select="@sname"/>
	</xsl:template>

	<xsl:template match="link[@name='link']//use[@name='table']" mode="normal" priority="2">
		<x:value-of select="@table"/>
	</xsl:template>

	<xsl:template match="link[@name='beforeinsert']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='before:insert']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='beforeinsert']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='before:insert']"/>
	</xsl:template>

	<xsl:template match="link[@name='afterinsert']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='after:insert']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='afterinsert']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='after:insert']"/>
	</xsl:template>

	<xsl:template match="link[@name='beforeupdate']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='before:update']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='beforeupdate']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='before:update']"/>
	</xsl:template>

	<xsl:template match="link[@name='afterupdate']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='after:update']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='afterupdate']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='after:update']"/>
	</xsl:template>

	<xsl:template match="link[@name='beforedelete']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='before:delete']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='beforedelete']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='before:delete']"/>
	</xsl:template>

	<xsl:template match="link[@name='afterdelete']" mode="normal" priority="1">
		<x:if test="triggers/trigger[@event='after:delete']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='afterdelete']" mode="normal" priority="1">
		<x:value-of select="triggers/trigger[@event='after:delete']"/>
	</xsl:template>

	<xsl:template match="link[@name='verify-inherited']" mode="normal" priority="1">
		<xsl:if test="not($mode='soft')">
			<x:if test="columns/column[not(@new='true')]">
				<xsl:apply-templates mode="normal"/>
			</x:if>
		</xsl:if>
	</xsl:template>

	<xsl:template match="link[@name='compositions']" mode="normal" priority="1">
		<x:if test="columns/column[@composition='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='compositions']//use[@name='composition']" mode="normal" priority="2">
		<x:for-each select="columns/column[@composition='true']">
			<item><xsl:apply-templates mode="resolv" select="(./ancestor::var[@name='composition'][1]|./preceding::var[@name='composition'][1])[last()]"/></item>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='inherit']" mode="normal" priority="1">
		<x:if test="columns/column[@inherit='true']">
			<xsl:apply-templates mode="normal"/>
		</x:if>
	</xsl:template>

	<xsl:template match="link[@name='inherit']//link[@name='subtype']//use[@name='inherit']" mode="normal" priority="3">
		<x:for-each select="/table/columns/column[@inherit='true']">
			<item><x:value-of select="@name"/>.<xsl:apply-templates mode="resolv" select="(./ancestor::var[@name='inherit'][1]|./preceding::var[@name='inherit'][1])[last()]"/></item>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='compositions']//use[@name='table']" mode="normal" priority="1">
		<x:value-of select="@table"/>
	</xsl:template>

	<xsl:template match="link[@name='set']" mode="normal" priority="1">
		<x:for-each select="/table/path/entry | entry">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="link[@name='path']" mode="normal" priority="1">
		<x:for-each select="/table/path/entry">
			<xsl:apply-templates mode="normal"/>
		</x:for-each>
	</xsl:template>

	<xsl:template match="use[@name='string']" mode="normal" priority="1">
		<x:value-of select="@string"/>
	</xsl:template>

	<xsl:template match="use[@name='value']" mode="normal" priority="1">
		<x:value-of select="@value"/>
		<x:if test="not(@value)">
			<x:if test="@string">
				<xsl:apply-templates mode="resolv" select="(./ancestor::var[@name='string'][1]|./preceding::var[@name='string'][1])[last()]"/>
			</x:if>
			<x:if test="not(@string)">
				<xsl:apply-templates mode="resolv" select="(./ancestor::var[@name='null'][1]|./preceding::var[@name='null'][1])[last()]"/>
			</x:if>
		</x:if>
	</xsl:template>

	<xsl:template match="use[@name='into']" mode="normal" priority="1">
		<x:value-of select="/table/insertions/@into"/>
	</xsl:template>

</xsl:stylesheet>
