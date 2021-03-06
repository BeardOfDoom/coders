package hu.unideb.inf.coders.service;

import hu.unideb.inf.coders.dto.LevelDTO;
import hu.unideb.inf.coders.dto.UserAttackDTO;
import hu.unideb.inf.coders.dto.UserDTO;
import hu.unideb.inf.coders.util.SkillUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

@Service
public class UserAttackManagerImpl implements UserAttackManager {

    private final int ATTACK_ENERGY_REQUIREMENT = 10;
    private final int ATTACK_TIME_IN_MINUTES = 60;

    @Autowired
    private SkillUtil skillUtil;

    @Autowired
    private JobManager jobManager;

    @Autowired
    private EnergyService energyService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAttackService userAttackService;

    @Autowired
    private LevelService levelService;

    @Autowired
    private MoneyService moneyService;

    @Autowired
    private XPService xpService;

    @Autowired
    private SkillService skillService;

    @Override
    public boolean canStartAttack(UserDTO attackerUserDTO, UserDTO defenderUserDTO) {

        if(isAttackingItself(attackerUserDTO, defenderUserDTO)) return false;

        if (isThereAlreadyActiveJob(attackerUserDTO)) return false;

        if (isThereAlreadyActiveAttack(attackerUserDTO)) return false;

        if (!isEnergyEnough(attackerUserDTO)) return false;

        if (!areSkillRequirementsMet(attackerUserDTO)) return false;

        if(!areLevelDifferencesProper(attackerUserDTO, defenderUserDTO)) return false;

        return true;

    }

    @Override
    public UserAttackDTO startAttack(UserDTO attackerUserDTO, UserDTO defenderUserDTO) {

        energyService.decreaseEnergy(attackerUserDTO, ATTACK_ENERGY_REQUIREMENT);

        UserAttackDTO userAttackDTO = new UserAttackDTO();
        userAttackDTO.setAttackerId(attackerUserDTO.getId());
        userAttackDTO.setDefenderId(defenderUserDTO.getId());
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime finish = LocalDateTime.now().plusMinutes(ATTACK_TIME_IN_MINUTES);
        userAttackDTO.setStart(start);
        userAttackDTO.setFinish(finish);
        userAttackDTO.setGainedRewards(false);

        userService.save(attackerUserDTO);
        userAttackService.save(userAttackDTO);

        return userAttackDTO;

    }

    @Override
    public boolean canFinishAttack(UserAttackDTO userAttackDTO) {

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(userAttackDTO.getFinish())) return false;

        if (userAttackDTO.isGainedRewards()) return false;

        return true;
    }

    @Override
    public UserDTO finishAttack(UserAttackDTO userAttackDTO) {

        UserDTO attackerUserDTO = userService.getUserById(userAttackDTO.getAttackerId());
        UserDTO defenderUserDTO = userService.getUserById(userAttackDTO.getDefenderId());

        UserDTO winnerUserDTO = getAttackWinner(attackerUserDTO, defenderUserDTO);
        LevelDTO levelDTO = levelService.findByLevel(winnerUserDTO.getLevel());

        if(winnerUserDTO.getId() == attackerUserDTO.getId()) {
            winnerUserDTO.setSuccessfulAttacks(attackerUserDTO.getSuccessfulAttacks() + 1);
        } else {
            attackerUserDTO.setUnsuccessfulAttacks(attackerUserDTO.getUnsuccessfulAttacks() + 1);
        }

        xpService.gain(winnerUserDTO, calculateAttackWinnerXpGain(), levelDTO);

        moneyService.increaseMoney(winnerUserDTO, calculateAttackWinnerMoneyGain());

        userAttackDTO.setGainedRewards(true);

        userService.save(attackerUserDTO);
        userService.save(winnerUserDTO);
        userAttackService.save(userAttackDTO);

        return winnerUserDTO;

    }

    @Override
    public UserAttackDTO getActiveAttack(UserDTO attackerUserDTO) {

        return userAttackService.getActiveAttack(attackerUserDTO);

    }

    public boolean isAttackingItself(UserDTO attackerUserDTO, UserDTO defenderUserDTO) {

        return attackerUserDTO.getId() == defenderUserDTO.getId();

    }

    public boolean isThereAlreadyActiveJob(UserDTO userDTO) {

        return jobManager.getActiveJob(userDTO) != null;

    }

    public boolean isThereAlreadyActiveAttack(UserDTO attackerUserDTO) {

        return getActiveAttack(attackerUserDTO) != null;

    }

    public boolean isEnergyEnough(UserDTO attackerUserDTO) {

        return attackerUserDTO.getEnergy() >= ATTACK_ENERGY_REQUIREMENT;

    }

    public boolean areSkillRequirementsMet(UserDTO attackerUserDTO) {

        return skillUtil.areSkillRequirementsMet(attackerUserDTO.getSkills(), "2,4,6,7,10");

    }

    public boolean areLevelDifferencesProper(UserDTO attackerUserDTO, UserDTO defenderUserDTO) {

        return Math.abs(attackerUserDTO.getLevel() - defenderUserDTO.getLevel()) <= 5;

    }

    public UserDTO getAttackWinner(UserDTO attackerUserDTO, UserDTO defenderUserDTO) {

        String attackerSkills = attackerUserDTO.getSkills();

        Set<Long> attackerSkillIds = skillUtil.extractSkillIds(attackerSkills);

        int attackSkillCounter = 0;

        for(Long attackerSkillId : attackerSkillIds) {

            String skillType = skillService.findById(attackerSkillId).getType();

            if (skillType.equals("OFFENSIVE") || skillType.equals("ADAPTIVE")) {
                attackSkillCounter++;
            }

        }

        String defenderSkills = attackerUserDTO.getSkills();

        Set<Long> defenderSkillIds = skillUtil.extractSkillIds(defenderSkills);

        int defendSkillCounter = 0;

        for(Long defenderSkillId : defenderSkillIds) {

            String skillType = skillService.findById(defenderSkillId).getType();

            if (skillType.equals("DEFENSIVE") || skillType.equals("ADAPTIVE")) {
                defendSkillCounter++;
            }

        }

        Random r = new Random();
        double attackerLuck = 1.0 + (1.4 - 1.0) * r.nextDouble();
        double defenderLuck = 1.0 + (1.4 - 1.0) * r.nextDouble();

        if (attackSkillCounter * attackerLuck >= defendSkillCounter * defenderLuck) {
            return attackerUserDTO;
        } else {
            return defenderUserDTO;
        }

    }

    public int calculateAttackWinnerXpGain() {

        return XP_GAIN;

    }

    public int calculateAttackWinnerMoneyGain() {

        return MONEY_GAIN;

    }

}
