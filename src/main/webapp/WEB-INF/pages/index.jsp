<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title> Live and let Flight </title>
    <link rel="stylesheet" href="/resources/css/mystyle.css">
    <script type="text/javascript" src="/resources/js/libs/jquery-1.10.2.js"></script>
    <script type="text/javascript" src="/resources/js/libs/handlebars-1.1.2.js"></script>
    <script type="text/javascript" src="/resources/js/libs/ember-1.4.0.js"></script>
    <script type="text/javascript" src="/resources/js/app.js"></script>
</head>
<body>
<script type="text/x-handlebars">
    {{outlet}}
  </script>

<%--<script type="text/x-handlebars" id="form">--%>
<div class="configurationArea">
    <div class="topConfiguration">
        <div class="optionConf"> <%--{{#link-to 'about'}}--%> Round Trip <%--{{/link-to}}--%> </div>
        <div class="optionConf"> One Way </div>
    </div>
    <div class="middleConfiguration" >
        <form method="get" class="daForm" action="">
        <div class="config1">
            <input type="text" class="input" name="sourceFlight" id="sourceFlight" size="31" maxlength="255" >  </input>&nbsp;
            <%--{{input type="text" value=from }}--%>
            <input type="text" class="input" name="destinyFlight" id="destinyFlight" size="31" maxlength="255" >  </input>&nbsp;
            <select id="firstDay" class="firstDay" name="firstDay">
                <option value="1" selected> Tomorrow </option>
                <option value="2"> Next Week  </option>
                <option value="3"> Next Month </option>
            </select>
            <select id="secondDay" class="secondDay" name="secondDay">
                <option value="1" selected> Next Week </option>
                <option value="2"> Next Month </option>
            </select>
            </div>
            <div class="config2" >
                <input type="button" value="Go" class="buttonF" />
            </div>
        </form>
    </div>
</div>
<%--</script>--%>

<script type="text/x-handlebars" id="index">
<div class="resultsArea">
{{#each}}
<div class="result even">
        <div class="section1">
            <div class="price">$ {{price}}</div>
            <div class="type">{{type}}</div>
        </div>
        <div class="section2">
            <div class="numbers">{{estimateDate1}} - {{estimateDate2}}</div>
            <div class="information">{{companies}}</div>
        </div>
        <div class="section3">
            <div class="numbers">{{estimateTimeTravel}}</div>
            <div class="information">{{airports}}</div>
        </div>
        <div class="section4">
            <div class="numbers">{{stops}}</div>
            <div class="information">{{scales}}</div>
        </div>
    </div>
    {{/each}}
    </div>
</script>

</body>
</html>