package org.sonar.plugins.findbugs.profiles;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.java.Java;

import java.io.InputStreamReader;
import java.io.Reader;

public class FindbugsContribProfile implements BuiltInQualityProfilesDefinition {

  public static final String FB_CONTRIB_PROFILE_NAME = "FindBugs + FB-Contrib";
  private final FindbugsProfileImporter importer;

  public FindbugsContribProfile(FindbugsProfileImporter importer) {
    this.importer = importer;
  }

  @Override
  public void define(Context context) {
    Reader findbugsProfile = new InputStreamReader(this.getClass().getResourceAsStream(
            "/org/sonar/plugins/findbugs/profile-findbugs-and-fb-contrib.xml"));
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(FB_CONTRIB_PROFILE_NAME, Java.KEY);
    importer.importProfile(findbugsProfile, profile);

    profile.done();
  }

}
