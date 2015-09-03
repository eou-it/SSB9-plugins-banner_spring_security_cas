<!--
/*******************************************************************************
Copyright 2009-2012 Ellucian Company L.P. and its affiliates.
*******************************************************************************/
-->
<html>
<head>
	<title>Banner Thirdparty Authentication Home Page</title>
	<style type="text/css">
	.message {
		border: 1px solid black;
		padding: 5px;
		background-color:#E9E9E9;
	}
	.stack {
		border: 1px solid black;
		padding: 5px;
		overflow:auto;
		height: 300px;
	}
	.snippet {
		padding: 5px;
		background-color:white;
		border:1px solid black;
		margin:3px;
		font-family:courier;
	}
	</style>
</head>

<body>
<h1>Authentication Information</h1>
<sec:ifLoggedIn>
	Welcome Back <sec:username/>!
</sec:ifLoggedIn>
<sec:ifNotLoggedIn>
	No User is logged-in.
</sec:ifNotLoggedIn>

</body>
</html>