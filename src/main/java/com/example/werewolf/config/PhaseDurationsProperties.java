package com.example.werewolf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "werewolf.phase")
public class PhaseDurationsProperties {
  private int wolves = 60;
  private int seer = 45;
  private int witch = 60;
  private int chat = 90;
  private int vote = 45;

  public int getWolves() { return wolves; }
  public void setWolves(int wolves) { this.wolves = wolves; }

  public int getSeer() { return seer; }
  public void setSeer(int seer) { this.seer = seer; }

  public int getWitch() { return witch; }
  public void setWitch(int witch) { this.witch = witch; }

  public int getChat() { return chat; }
  public void setChat(int chat) { this.chat = chat; }

  public int getVote() { return vote; }
  public void setVote(int vote) { this.vote = vote; }
}
