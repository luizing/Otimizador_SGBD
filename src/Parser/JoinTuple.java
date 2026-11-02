package Parser;

public record JoinTuple(
        String table,
        String att1,
        String relationship,
        String att2
) {}