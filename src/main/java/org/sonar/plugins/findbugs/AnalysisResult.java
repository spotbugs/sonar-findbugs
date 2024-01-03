/*
 * SonarQube Findbugs Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.umd.cs.findbugs.AnalysisError;
import edu.umd.cs.findbugs.BugInstance;

/**
 * @author gtoison
 *
 */
public class AnalysisResult {
  private Collection<ReportedBug> reportedBugs;
  private Collection<AnalysisError> analysisErrors;
  
  public AnalysisResult() {
    this(new ArrayList<>(), new ArrayList<>());
  }
  
  public AnalysisResult(BugInstance bugInstance) {
    this(Arrays.asList(new ReportedBug(bugInstance)), new ArrayList<>());
  }
  
  public AnalysisResult(Collection<ReportedBug> reportedBugs, Collection<? extends AnalysisError> errors) {
    this.reportedBugs = reportedBugs;
    this.analysisErrors = new ArrayList<>(errors);
  }

  public Collection<ReportedBug> getReportedBugs() {
    return reportedBugs;
  }
  
  public Collection<AnalysisError> getAnalysisErrors() {
    return analysisErrors;
  }
}
