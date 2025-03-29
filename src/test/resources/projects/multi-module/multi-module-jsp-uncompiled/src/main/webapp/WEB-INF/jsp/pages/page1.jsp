<%
	String x = request.getParameter("x");

	java.util.Comparator comparator = new java.util.Comparator() {
		public int compare(Object o1, Object o2) {
			return 0;
		}
	};

%>
<p><%=x%></p>
