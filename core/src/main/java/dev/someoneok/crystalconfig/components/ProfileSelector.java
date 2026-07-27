package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.config.ProfileConfig;
import dev.someoneok.crystalconfig.containers.Column;
import dev.someoneok.crystalconfig.containers.Row;
import dev.someoneok.crystalconfig.state.Binding;
import dev.someoneok.crystalconfig.state.MutableState;

import java.util.List;
import java.util.Objects;

public final class ProfileSelector extends Column {
    private static final String ADD_PROFILE_ACTION = "__crystal_config_add_profile_action__";
    private enum EditMode { ADD, RENAME }

    private final ProfileConfig profiles;
    private final Dropdown<String> dropdown;
    private final Button renameButton = new Button("✎");
    private final Button deleteButton = new Button("🗑");
    private final MutableState<String> nameState = new MutableState<>("");
    private final TextInput nameInput = new TextInput(nameState);
    private final Button saveButton = new Button("Save").accent(true);
    private final Button cancelButton = new Button("Cancel");
    private final Row editorRow = new Row().gap(6).fillX();
    private EditMode editMode;
    private List<String> lastIds = List.of();

    public ProfileSelector(ProfileConfig profiles) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        gap(7);
        width(260);

        dropdown = new Dropdown<>(Binding.of(profiles::selectedId, this::selectDropdownEntry), dropdownEntries())
                .labeler(this::labelDropdownEntry)
                .maxVisibleRows(7);
        dropdown.flex(1).height(30);

        renameButton.size(34, 30); renameButton.minSize(34, 30); renameButton.tooltip("Rename selected profile"); renameButton.onClick(this::openRename);
        deleteButton.size(34, 30); deleteButton.minSize(34, 30); deleteButton.tooltip("Delete selected profile"); deleteButton.onClick(this::deleteSelected);

        Row selectorRow = new Row().gap(6).fillX();
        selectorRow.add(dropdown);
        selectorRow.add(renameButton);
        selectorRow.add(deleteButton);

        nameInput.placeholder("Profile name").maxLength(ProfileConfig.MAX_NAME_CODE_POINTS * 2)
                .validator(value -> !ProfileConfig.sanitizeName(value).isBlank())
                .onCommit(ignored -> saveEditor())
                .flex(1).height(28);
        saveButton.size(54, 28); saveButton.minSize(54, 28); saveButton.onClick(this::saveEditor);
        cancelButton.size(60, 28); cancelButton.minSize(60, 28); cancelButton.onClick(this::closeEditor);
        editorRow.add(nameInput);
        editorRow.add(saveButton);
        editorRow.add(cancelButton);
        editorRow.visible(false);

        add(selectorRow);
        add(editorRow);
        refreshProfiles(true);
    }

    @Override
    public void tick(float deltaSeconds) {
        refreshProfiles(false);
        deleteButton.enabled(profiles.canDelete());
        super.tick(deltaSeconds);
    }

    private void refreshProfiles(boolean force) {
        List<String> ids = profileIds();
        if (force || !ids.equals(lastIds)) {
            lastIds = ids;
            dropdown.options(dropdownEntries());
        }
    }

    private List<String> profileIds() {
        return profiles.profiles().stream().map(ProfileConfig.Profile::id).toList();
    }

    private List<String> dropdownEntries() {
        List<String> entries = new java.util.ArrayList<>(profileIds());
        entries.add(ADD_PROFILE_ACTION);
        return entries;
    }

    private String labelDropdownEntry(String id) {
        return ADD_PROFILE_ACTION.equals(id) ? "+ Add new profile" : profiles.nameOf(id);
    }

    private void selectDropdownEntry(String id) {
        if (ADD_PROFILE_ACTION.equals(id)) openAdd();
        else profiles.select(id);
    }

    private void openAdd() {
        editMode = EditMode.ADD;
        nameState.set("");
        openEditor();
    }

    private void openRename() {
        editMode = EditMode.RENAME;
        nameState.set(profiles.selectedName());
        openEditor();
    }

    private void openEditor() {
        editorRow.visible(true);
        nameInput.setFocused(true);
        markLayoutDirty();
    }

    private void closeEditor() {
        editMode = null;
        nameInput.setFocused(false);
        editorRow.visible(false);
        markLayoutDirty();
    }

    private void saveEditor() {
        String safe = ProfileConfig.sanitizeName(nameState.get());
        if (safe.isBlank() || editMode == null) return;
        if (editMode == EditMode.ADD) profiles.add(safe);
        else profiles.rename(profiles.selectedId(), safe);
        refreshProfiles(true);
        closeEditor();
    }

    private void deleteSelected() {
        if (!profiles.canDelete()) return;
        profiles.delete(profiles.selectedId());
        refreshProfiles(true);
        closeEditor();
    }
}
