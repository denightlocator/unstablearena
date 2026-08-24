package com.unstable.characters.managers;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CharacterManager {

    private final UnstableCharacters plugin;
    private final Map<String, Character> characters;
    private final Map<String, String> claimedByCharacter;
    private final File charactersFile;

    public CharacterManager(UnstableCharacters plugin) {
        this.plugin = plugin;
        this.characters = new HashMap<>();
        this.claimedByCharacter = new HashMap<>();
        this.charactersFile = new File(plugin.getDataFolder(), "characters.yml");

        loadCharacters();
        createDefaultCharacters();
    }

    private void loadCharacters() {
        if (!charactersFile.exists()) {
            try {
                charactersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create characters.yml: " + e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(charactersFile);
        ConfigurationSection section = config.getConfigurationSection("characters");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection charSection = section.getConfigurationSection(id);
                if (charSection != null) {
                    Character character = new Character(
                        id,
                        charSection.getString("displayName", id),
                        charSection.getString("clan", "None")
                    );
                    character.setPrefix(charSection.getString("prefix", ""));
                    character.setSuffix(charSection.getString("suffix", ""));
                    character.setChatColor(charSection.getString("chatColor", "white"));
                    character.setDescription(charSection.getString("description", ""));
                    character.setLore(charSection.getStringList("lore"));
                    character.setTabPrefix(charSection.getString("tabPrefix", ""));
                    character.setTabSuffix(charSection.getString("tabSuffix", ""));
                    character.setGlowEnabled(charSection.getBoolean("glow.enabled", false));
                    character.setGlowColor(charSection.getString("glow.color", "white"));
                    character.setAvailable(charSection.getBoolean("available", true));

                    characters.put(id.toLowerCase(), character);
                }
            }
        }
    }

    private void createDefaultCharacters() {
        // Only create if file is empty
        if (characters.isEmpty()) {
            plugin.getLogger().info("Creating Unstable SMP characters...");

            // =============================================
            // CENTRAL PROTAGONISTS
            // =============================================

            // Wemmbu - The Architect & Empire Builder
            addCharacterData("wemmbu", "Wemmbu", "Protagonists",
                ChatColor.AQUA + "[Builder] ", ChatColor.WHITE + "",
                "aqua", "The Architect & Empire Builder. Central protagonist of Unstable SMP.", true);

            // ParrotX2 - The Master Hunter & Strategist
            addCharacterData("parrotx2", "ParrotX2", "Protagonists",
                ChatColor.GREEN + "[Hunter] ", ChatColor.DARK_GREEN + "",
                "green", "The Master Hunter & Strategist. One of the four central protagonists.", true);

            // SpokeIsHere - The Survival Specialist
            addCharacterData("spokeishere", "SpokeIsHere", "Protagonists",
                ChatColor.YELLOW + "[Survival] ", ChatColor.GOLD + "",
                "yellow", "The Survival Specialist. Central protagonist of Unstable SMP.", true);

            // FlameFrags - The Combat Elite / Lone Wolf
            addCharacterData("flamefrags", "FlameFrags", "Protagonists",
                ChatColor.RED + "[Combat] ", ChatColor.DARK_RED + "",
                "red", "The Combat Elite & Lone Wolf. Central protagonist of Unstable SMP.", true);

            // =============================================
            // MAJOR ALLIES & RECURRING FIGURES
            // =============================================

            addCharacterData("wifies", "Wifies", "Allies",
                ChatColor.LIGHT_PURPLE + "[Ally] ", ChatColor.DARK_PURPLE + "",
                "light_purple", "A highly influential figure who has alternated between ally and antagonist.", true);

            addCharacterData("eggchan", "Eggchan", "Allies",
                ChatColor.WHITE + "[Egg] ", ChatColor.YELLOW + "",
                "white", "A frequent teammate and ally to the protagonists.", true);

            addCharacterData("mapicc", "Mapicc", "Allies",
                ChatColor.BLUE + "[Builder] ", ChatColor.DARK_BLUE + "",
                "blue", "A skilled player known for large-scale projects and defense.", true);

            addCharacterData("theobaldthebird", "TheobaldTheBird", "Allies",
                ChatColor.GOLD + "[Tech] ", ChatColor.YELLOW + "",
                "gold", "Known for technical support and lore contributions.", true);

            addCharacterData("lomedy", "Lomedy", "Allies",
                ChatColor.AQUA + "[Ally] ", ChatColor.BLUE + "",
                "aqua", "A consistent presence in the protagonists' circles.", true);

            addCharacterData("spongs", "Spongs", "Allies",
                ChatColor.GRAY + "[Ally] ", ChatColor.DARK_GRAY + "",
                "gray", "A recurring character involved in various server arcs.", true);

            addCharacterData("rejoicin", "Rejoicin", "Allies",
                ChatColor.WHITE + "[Ally] ", ChatColor.GOLD + "",
                "white", "Often seen in major group events and specific lore chapters.", true);

            addCharacterData("reinadrop", "ReinaDrop", "Allies",
                ChatColor.LIGHT_PURPLE + "[Ally] ", ChatColor.AQUA + "",
                "light_purple", "A recurring figure in later seasons and specific story arcs.", true);

            // =============================================
            // PRIMARY ANTAGONISTS
            // =============================================

            // PrinceZam - Leader of Zam Empire
            addCharacterData("princezam", "PrinceZam", "Zam Empire",
                ChatColor.DARK_RED + "[Zam] ", ChatColor.RED + "",
                "dark_red", "Leader of the Zam Empire and primary antagonist during the Empire War arc.", true);

            // The NULL - Mysterious Threat
            addCharacterData("thenull", "The NULL", "NULL",
                ChatColor.DARK_PURPLE + "[NULL] ", ChatColor.BLACK + "",
                "dark_purple", "A mysterious and threatening presence that acts as a catalyst for chaos.", true);

            // ClownPierce - Legendary Combatant
            addCharacterData("clownpierce", "ClownPierce", "Antagonists",
                ChatColor.DARK_RED + "[Clown] ", ChatColor.WHITE + "",
                "dark_red", "A legendary combatant known for being a major threat to the protagonists.", true);

            // JamatoP - Recurring Antagonist
            addCharacterData("jamatop", "JamatoP", "Antagonists",
                ChatColor.RED + "[Jama] ", ChatColor.GOLD + "",
                "red", "A recurring antagonist who has challenged the protagonists' dominance.", true);

            // Ashswagg - Notable Antagonist
            addCharacterData("ashswagg", "Ashswagg", "Antagonists",
                ChatColor.DARK_GRAY + "[Ashes] ", ChatColor.RED + "",
                "dark_gray", "A notable antagonist involved in high-stakes conflicts and betrayals.", true);

            // Jaden_MAN - Long-standing Figure
            addCharacterData("jadenman", "Jaden_MAN", "Antagonists",
                ChatColor.GOLD + "[Jaden] ", ChatColor.DARK_RED + "",
                "gold", "A long-standing figure who has served as both threat and recurring presence.", true);

            // =============================================
            // THE MAFIA
            // =============================================

            addCharacterData("thegodofwar", "TheGodOfWar", "Mafia",
                ChatColor.BLACK + "[Mafia] ", ChatColor.DARK_RED + "",
                "black", "A member of the Mafia, acting as collective antagonist in early seasons.", true);

            addCharacterData("deputyace", "Deputy_Ace", "Mafia",
                ChatColor.BLUE + "[Mafia] ", ChatColor.DARK_BLUE + "",
                "blue", "A member of the Mafia organization.", true);

            // =============================================
            // ZAM EMPIRE SOLDIERS
            // =============================================

            addCharacterData("sargelaw", "SargeLAW", "Zam Empire",
                ChatColor.DARK_RED + "[Zam] ", ChatColor.RED + "",
                "dark_red", "A soldier of the Zam Empire.", true);

            addCharacterData("horacealtman", "Horace Altman", "Zam Empire",
                ChatColor.GOLD + "[Zam] ", ChatColor.YELLOW + "",
                "gold", "A member of PrinceZam's Zam Empire.", true);

            // =============================================
            // SAPARATA / CINDERCREST
            // =============================================

            addCharacterData("saparata", "Saparata", "Cindercrest",
                ChatColor.GOLD + "[King] ", ChatColor.YELLOW + "",
                "gold", "Leader of Cindercrest - a top-tier figure of Unstable SMP.", true);

            // =============================================
            // OTHER NOTABLE CHARACTERS
            // =============================================

            addCharacterData("minutetech", "MinuteTech", "Technicians",
                ChatColor.AQUA + "[Tech] ", ChatColor.BLUE + "",
                "aqua", "A technical player who assists with complex builds.", true);

            addCharacterData("manepear", "ManePear", "Builders",
                ChatColor.GREEN + "[Builder] ", ChatColor.DARK_GREEN + "",
                "green", "A skilled builder and collaborator.", true);

            addCharacterData("itzrealme", "ItzRealMe", "Neutral",
                ChatColor.WHITE + "[???] ", ChatColor.GRAY + "",
                "white", "A mysterious figure on the server.", true);

            addCharacterData("marlowww", "MarLowww", "Allies",
                ChatColor.YELLOW + "[Ally] ", ChatColor.GOLD + "",
                "yellow", "A recurring ally in the Unstable SMP.", true);

            addCharacterData("swight", "Swight", "Neutral",
                ChatColor.GREEN + "[Swight] ", ChatColor.DARK_GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("ferremc", "FerreMC", "Neutral",
                ChatColor.GRAY + "[Ferre] ", ChatColor.DARK_GRAY + "",
                "gray", "A participant in the Unstable SMP.", true);

            addCharacterData("mugm", "Mugm", "Neutral",
                ChatColor.DARK_GRAY + "[Mugm] ", ChatColor.GRAY + "",
                "dark_gray", "A participant in the Unstable SMP.", true);

            addCharacterData("jumperwho", "JumperWho", "Neutral",
                ChatColor.AQUA + "[Jumper] ", ChatColor.BLUE + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("wyll", "Wyll", "Neutral",
                ChatColor.RED + "[Wyll] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("purpled", "Purpled", "Neutral",
                ChatColor.DARK_PURPLE + "[Purple] ", ChatColor.LIGHT_PURPLE + "",
                "dark_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("leow0ok", "Leow0ok", "Neutral",
                ChatColor.GOLD + "[Leo] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("lettucek", "LettuceK", "Neutral",
                ChatColor.GREEN + "[Lettuce] ", ChatColor.GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("reddoons", "Reddoons", "Neutral",
                ChatColor.RED + "[Red] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("boomie", "Boomie", "Neutral",
                ChatColor.YELLOW + "[Boom] ", ChatColor.GOLD + "",
                "yellow", "A participant in the Unstable SMP.", true);

            addCharacterData("boosfer", "Boosfer", "Neutral",
                ChatColor.DARK_GRAY + "[Boos] ", ChatColor.GRAY + "",
                "dark_gray", "A participant in the Unstable SMP.", true);

            addCharacterData("conexion", "Conexion", "Neutral",
                ChatColor.AQUA + "[Conex] ", ChatColor.BLUE + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("falconu", "FalconU", "Neutral",
                ChatColor.GOLD + "[Falcon] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("nufuli", "Nufuli", "Neutral",
                ChatColor.LIGHT_PURPLE + "[Nufuli] ", ChatColor.AQUA + "",
                "light_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("teamkalal", "TeamKalal", "Neutral",
                ChatColor.GREEN + "[Team] ", ChatColor.DARK_GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("jepexx", "Jepexx", "Neutral",
                ChatColor.RED + "[Jepex] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("fantst", "Fantst", "Neutral",
                ChatColor.AQUA + "[Fant] ", ChatColor.BLUE + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("baablu", "Baablu", "Neutral",
                ChatColor.GREEN + "[Baab] ", ChatColor.YELLOW + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("mrcube6", "MrCube6", "Neutral",
                ChatColor.BLUE + "[Cube] ", ChatColor.DARK_BLUE + "",
                "blue", "A participant in the Unstable SMP.", true);

            addCharacterData("onlyasquid", "Only_A_Squid", "Neutral",
                ChatColor.AQUA + "[Squid] ", ChatColor.AQUA + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("sharkilz", "Sharkilz", "Neutral",
                ChatColor.AQUA + "[Shark] ", ChatColor.AQUA + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("luke4472", "Luke4472", "Neutral",
                ChatColor.GRAY + "[Luke] ", ChatColor.DARK_GRAY + "",
                "gray", "A participant in the Unstable SMP.", true);

            addCharacterData("spepticle", "Spepticle", "Neutral",
                ChatColor.GOLD + "[Spept] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("truoriginal", "TruOriginal", "Neutral",
                ChatColor.WHITE + "[Tru] ", ChatColor.GRAY + "",
                "white", "A participant in the Unstable SMP.", true);

            addCharacterData("roshambogames", "Roshambogames", "Neutral",
                ChatColor.RED + "[Rosha] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("lopezzz", "Lopezzz", "Neutral",
                ChatColor.GOLD + "[Lopz] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("arcn", "Arcn", "Neutral",
                ChatColor.DARK_PURPLE + "[Arcn] ", ChatColor.LIGHT_PURPLE + "",
                "dark_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("willsion", "Willsion", "Neutral",
                ChatColor.GREEN + "[Will] ", ChatColor.DARK_GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("shoebilly", "ShoeBilly", "Cindercrest",
                ChatColor.GOLD + "[Billy] ", ChatColor.YELLOW + "",
                "gold", "Cindercrest warrior - ally of Saparata.", true);

            addCharacterData("fymada", "Fymada", "Neutral",
                ChatColor.AQUA + "[Fym] ", ChatColor.BLUE + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("luigitoan", "LuigiToan", "Neutral",
                ChatColor.GREEN + "[Luigi] ", ChatColor.DARK_GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("deanthebean9", "deanthebean9", "Neutral",
                ChatColor.YELLOW + "[Dean] ", ChatColor.GOLD + "",
                "yellow", "A participant in the Unstable SMP.", true);

            addCharacterData("peentar", "Peentar", "Neutral",
                ChatColor.GOLD + "[Peen] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("woogie", "Woogie", "Neutral",
                ChatColor.LIGHT_PURPLE + "[Woogie] ", ChatColor.AQUA + "",
                "light_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("therealsquiddo", "TheRealSquiddo", "Neutral",
                ChatColor.AQUA + "[Squiddo] ", ChatColor.AQUA + "",
                "aqua", "A participant in the Unstable SMP.", true);

            addCharacterData("rekrap2", "Rekrap2", "Neutral",
                ChatColor.RED + "[Rek] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("pangi", "Pangi", "Neutral",
                ChatColor.GOLD + "[Pangi] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            addCharacterData("branzycraft", "BranzyCraft", "Neutral",
                ChatColor.GREEN + "[Branzy] ", ChatColor.DARK_GREEN + "",
                "green", "A participant in the Unstable SMP.", true);

            addCharacterData("sirpig", "SirPig", "Neutral",
                ChatColor.LIGHT_PURPLE + "[Pig] ", ChatColor.LIGHT_PURPLE + "",
                "light_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("salvationism", "Salvationism", "Neutral",
                ChatColor.WHITE + "[Salva] ", ChatColor.GOLD + "",
                "white", "A participant in the Unstable SMP.", true);

            addCharacterData("tai", "Tai", "Neutral",
                ChatColor.RED + "[Tai] ", ChatColor.DARK_RED + "",
                "red", "A participant in the Unstable SMP.", true);

            addCharacterData("wallibear", "Wallibear", "Neutral",
                ChatColor.YELLOW + "[Walls] ", ChatColor.GOLD + "",
                "yellow", "A participant in the Unstable SMP.", true);

            addCharacterData("hannahxxrose", "Hannahxxrose", "Neutral",
                ChatColor.LIGHT_PURPLE + "[Hannah] ", ChatColor.AQUA + "",
                "light_purple", "A participant in the Unstable SMP.", true);

            addCharacterData("sarpn", "Sarpn", "Neutral",
                ChatColor.DARK_GRAY + "[Sarpn] ", ChatColor.GRAY + "",
                "dark_gray", "A participant in the Unstable SMP.", true);

            addCharacterData("yungwill", "Yungwill", "Neutral",
                ChatColor.GOLD + "[Yung] ", ChatColor.YELLOW + "",
                "gold", "A participant in the Unstable SMP.", true);

            // =============================================
            // LAW ENFORCEMENT
            // =============================================

            addCharacterData("thelaw", "The Law", "Law Enforcement",
                ChatColor.BLUE + "[Law] ", ChatColor.DARK_BLUE + "",
                "blue", "A member of the server's law enforcement.", true);

            // =============================================
            // THE SOUL KEEPERS
            // =============================================

            addCharacterData("director", "The Director", "Soul Keepers",
                ChatColor.DARK_PURPLE + "[Director] ", ChatColor.BLACK + "",
                "dark_purple", "A mysterious member of the Soul Keepers tied to supernatural lore.", true);

            addCharacterData("soulkeeper", "Soul Keeper", "Soul Keepers",
                ChatColor.DARK_GRAY + "[Soul] ", ChatColor.BLACK + "",
                "dark_gray", "A mysterious figure tied to the server's deeper supernatural lore.", true);

            // =============================================
            // KINGDOM OF THE CAVES (Arachnid)
            // =============================================

            addCharacterData("arachn1d", "Arachn1d", "Kingdom of the Caves",
                ChatColor.GREEN + "[Caves] ", ChatColor.DARK_GREEN + "",
                "green", "Leader of the Kingdom of the Caves - underground faction.", true);

            saveCharacters();
            plugin.getLogger().info("Loaded " + characters.size() + " Unstable SMP characters!");
        }
    }

    private void addCharacterData(String id, String displayName, String clan,
            String prefix, String suffix, String chatColor, String description, boolean available) {
        Character character = new Character(id, displayName, clan);
        character.setPrefix(prefix);
        character.setSuffix(suffix);
        character.setChatColor(chatColor);
        character.setDescription(description);
        character.setAvailable(available);
        characters.put(id.toLowerCase(), character);
    }

    public void saveCharacters() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(charactersFile);
        config.set("characters", null);

        for (Character character : characters.values()) {
            String path = "characters." + character.getId();
            config.set(path + ".displayName", character.getDisplayName());
            config.set(path + ".clan", character.getClan());
            config.set(path + ".prefix", character.getPrefix());
            config.set(path + ".suffix", character.getSuffix());
            config.set(path + ".chatColor", character.getChatColor());
            config.set(path + ".description", character.getDescription());
            config.set(path + ".lore", character.getLore());
            config.set(path + ".tabPrefix", character.getTabPrefix());
            config.set(path + ".tabSuffix", character.getTabSuffix());
            config.set(path + ".glow.enabled", character.isGlowEnabled());
            config.set(path + ".glow.color", character.getGlowColor());
            config.set(path + ".available", character.isAvailable());
        }

        try {
            config.save(charactersFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save characters.yml: " + e.getMessage());
        }
    }

    public Character getCharacter(String id) {
        return characters.get(id.toLowerCase());
    }

    public Map<String, Character> getCharacters() {
        return new HashMap<>(characters);
    }

    public List<Character> getAvailableCharacters() {
        return characters.values().stream()
            .filter(Character::isAvailable)
            .toList();
    }

    public void claimCharacter(String characterId, UUID playerId) {
        claimedByCharacter.put(characterId.toLowerCase(), playerId.toString());
    }

    public void unclaimCharacter(String characterId) {
        claimedByCharacter.remove(characterId.toLowerCase());
    }

    public boolean isCharacterClaimed(String characterId) {
        return claimedByCharacter.containsKey(characterId.toLowerCase());
    }

    public String getCharacterOwner(String characterId) {
        return claimedByCharacter.get(characterId.toLowerCase());
    }

    public void removePlayerClaims(UUID playerId) {
        claimedByCharacter.entrySet().removeIf(e -> e.getValue().equals(playerId.toString()));
    }

    public Character addCharacter(String id, String displayName, String clan) {
        Character character = new Character(id, displayName, clan);
        characters.put(id.toLowerCase(), character);
        saveCharacters();
        return character;
    }

    public boolean removeCharacter(String id) {
        if (characters.remove(id.toLowerCase()) != null) {
            claimedByCharacter.remove(id.toLowerCase());
            saveCharacters();
            return true;
        }
        return false;
    }

    public void updateCharacter(Character character) {
        characters.put(character.getId().toLowerCase(), character);
        saveCharacters();
    }

    public List<Character> getCharactersByClan(String clan) {
        return characters.values().stream()
            .filter(c -> c.getClan().equalsIgnoreCase(clan))
            .toList();
    }

    public Set<String> getAllClans() {
        return characters.values().stream()
            .map(Character::getClan)
            .filter(clan -> !clan.equalsIgnoreCase("none"))
            .collect(Collectors.toSet());
    }
}
