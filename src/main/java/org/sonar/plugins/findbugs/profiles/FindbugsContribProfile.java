package org.sonar.plugins.findbugs.profiles;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.findbugs.FindbugsProfileImporter;
import org.sonar.plugins.java.Java;

import java.io.InputStreamReader;
import java.io.Reader;

public class FindbugsContribProfile extends ProfileDefinition {

  private static final String FB_CONTRIB_PROFILE_NAME = "FindBugs + FB-Contrib";
  private final FindbugsProfileImporter importer;

  public FindbugsContribProfile(FindbugsProfileImporter importer) {
    this.importer = importer;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages messages) {
    Reader findbugsProfile = new InputStreamReader(this.getClass().getResourceAsStream(
            "/org/sonar/plugins/findbugs/profile-findbugs-and-fb-contrib.xml"));
    RulesProfile profile = importer.importProfile(findbugsProfile, messages);
    profile.setLanguage(Java.KEY);
    profile.setName(FB_CONTRIB_PROFILE_NAME);
    return profile;
  }

}
