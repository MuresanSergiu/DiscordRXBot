package afterwind.relax_bot;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ColorChanger {

    private IGuild guild;
    private Map<Color, IRole> colorRoles = new HashMap<>();

    public ColorChanger(IGuild guild) {
        this.guild = guild;
        initColorRoles();
    }

    /**
     * Gets all the roles that already exist in the guild
     */
    private void initColorRoles() {
        for (IRole role : guild.getRoles()) {
            if (role.getName().startsWith("RX_Color")) {
                colorRoles.put(role.getColor(), role);
            }
        }
    }

    /**
     * Gets the color role with the given color
     * Creates the color role if it doesn't exist
     * @param color color
     * @return The role with the specified color
     */
    private IRole getRole(Color color) {
        try {
            IRole colorRole = colorRoles.get(color);
            if (colorRole == null) {
                colorRole = guild.createRole();
                colorRole.changeName(String.format("RX_Color_#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
                colorRole.changeColor(color);
                colorRole.changePermissions(EnumSet.noneOf(Permissions.class));
                colorRoles.put(color, colorRole);
            }
            return colorRole;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Gives a color to the IUser
     * @param color color
     * @param user user
     */
    private void giveColor(Color color, IUser user) {

        try {
            List<IRole> userRoles = user.getRolesForGuild(guild);
            for (int i = 0; i < userRoles.size(); i++) {
                IRole role = userRoles.get(i);
                if (role.getName().startsWith("RX_Color")) {
                    if (!role.getColor().equals(color)) {
                        user.removeRole(role);
                        i--;
                    }
                }
            }
            user.addRole(getRole(color));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Checks if there are any color roles that nobody uses and deletes them
     */
    public void checkRoles() {
        List<Color> toRemove = new ArrayList<>();
        try {
            for (Iterator<IRole> it = colorRoles.values().iterator(); it.hasNext();) {

                IRole role = it.next();
                boolean isUsed = false;
                for (IUser user : guild.getUsers()) {
                    if (user.getRolesForGuild(guild).contains(role)) {
                        isUsed = true;
                        break;
                    }
                }
                if (!isUsed) {
                    for (IUser user : guild.getUsers()) {
                        if (user.getRolesForGuild(guild).contains(role)) {
                            user.removeRole(role);
                        }
                    }
                    toRemove.add(role.getColor());
                }
            }
            for (Color color :toRemove) {
                colorRoles.get(color).delete();
                colorRoles.remove(color);
                System.out.println("Color " + color + " removed!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent ev) {
        if (!ev.getMessage().getGuild().equals(guild)) {
            return;
        }

        try {
            if (ev.getMessage().getMentions().contains(RelaxBot.client.getOurUser())) {
                IUser author = ev.getMessage().getAuthor();

                String[] split = ev.getMessage().getContent().split(" +");
                System.out.println(ev.getMessage().getContent());

                if (split[1].trim().equals("color")) {
                    if (split.length < 3) {
                        Utils.sendMessage(author, ev.getMessage().getChannel(), "command usage is *color #rrggbb*");
                        return;
                    }


                    String colorName = split[2].trim();
                    if (!colorName.startsWith("#")) {
                        Utils.sendMessage(author, ev.getMessage().getChannel(), "invalid color format, please use *#rrggbb* format");
                        return;
                    }
                    if (colorName.endsWith("000000")) {
                        Utils.sendMessage(author, ev.getMessage().getChannel(), "this color is used as 'default color' for Discord roles. Try *#000001* instead.");
                        return;
                    }

                    try {
                        Color color = Color.decode(colorName);
                        giveColor(color, author);
                    } catch (NumberFormatException ex) {
                        Utils.sendMessage(author, ev.getMessage().getChannel(), "invalid color format!");
                        return;
                    }
                    Utils.sendMessage(author, ev.getMessage().getChannel(), "your new color is now " + colorName);
                } else if (split[1].trim().equals("cleanup")) {
                    checkRoles();
                } else {
                    Utils.sendMessage(author, ev.getMessage().getChannel(), "this command does not exist!");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
