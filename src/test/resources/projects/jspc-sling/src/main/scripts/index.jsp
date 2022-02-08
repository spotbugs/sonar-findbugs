<!DOCTYPE html>
<HTML lang="en">
<HEAD>
<TITLE>Index</TITLE>
</HEAD>
<BODY>
	<%
	String x = request.getParameter("x");

	java.util.Comparator comparator = new java.util.Comparator() {
		public int compare(Object o1, Object o2) {
			return 0;
		}
	};

	%>
	
	<H2>Hello <%=x%></H2>
</BODY>
</HTML>