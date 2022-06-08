package com.dotcms.cdi;


import com.dotmarketing.util.Logger;

import javax.servlet.RequestDispatcher;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "GetCities", loadOnStartup=0, urlPatterns = {"/GetCities"})
public class GetCities extends HttpServlet {

    @Inject
    ICityService cityService;

    public void init() throws ServletException {
        // Do required initialization
        Logger.info(this,"Initializing CDI Test Servlet");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        // Set response content type
        response.setContentType("text/html");

        // Actual logic goes here.
        PrintWriter out = response.getWriter();

        out.println("<table>");
        out.print("<tr><th>Id</th><th>Name</th><th>Population</th></tr>");
        for (City city : cityService.getCities())
        {
            out.println("<tr>");
            out.print("<td>" + city.getId() + "</td>");
            out.print("<td>" + city.getName() + "</td>");
            out.print("<td>" + city.getPopulation() + "</td>");
            out.println("</tr>");
        }
        out.println("<table>");

    }
}